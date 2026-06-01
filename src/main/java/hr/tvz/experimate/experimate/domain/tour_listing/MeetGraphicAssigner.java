package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.shared.Constraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assigns a unique meet graphic (color index + symbol index) to a {@link TourListing} on the
 * first guest or host check-in. The pair is guaranteed to differ from all other listings within
 * {@link Constraints.MeetGraphicConstraints#PROXIMITY_METERS} metres and
 * ±{@link Constraints.MeetGraphicConstraints#TIME_WINDOW_HOURS} hours of the meeting date.
 *
 * <p><b>De-collision:</b> each {@code (color, symbol)} pair is encoded as a single integer
 * {@code enc = color * SYMBOL_COUNT + symbol} (range 0–599). First-fit walks from a seed
 * derived from the listing ID until it finds a value not taken by any nearby listing.
 * With 600 possible pairs and at most a handful of neighbours, this trivially succeeds.
 *
 * <p><b>Concurrency:</b> a Postgres advisory lock keyed on the ~1° geo-cell (~111 km)
 * serialises concurrent assignments for listings in the same area. Listings in different
 * cells (e.g. Zagreb vs New York) acquire different lock keys and proceed in parallel.
 * The advisory lock is transaction-scoped and releases automatically on commit or rollback.
 */
@Component
public class MeetGraphicAssigner {

    private static final Logger log = LoggerFactory.getLogger(MeetGraphicAssigner.class);

    private static final int TOTAL_PAIRS =
            Constraints.MeetGraphicConstraints.COLOR_COUNT * Constraints.MeetGraphicConstraints.SYMBOL_COUNT;

    private final TourListingRepo listingRepo;
    private final JdbcTemplate jdbcTemplate;

    public MeetGraphicAssigner(TourListingRepo listingRepo, JdbcTemplate jdbcTemplate) {
        this.listingRepo = listingRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Assigns a meet graphic to {@code listing} if one has not already been assigned.
     *
     * <p>Must be called from within an active transaction — the Postgres advisory lock is
     * transaction-scoped and will be released automatically when the caller's transaction
     * commits or rolls back.
     *
     * @param listing the listing to assign; re-fetched under the lock to guard against
     *                a concurrent transaction that may have assigned just before us
     */
    @Transactional
    public void assign(TourListing listing) {
        long lockKey = cellLockKey(listing.getLatitude(), listing.getLongitude());
        acquireAdvisoryLock(lockKey);

        // Re-fetch after acquiring the lock: a concurrent transaction on the same listing
        // may have committed an assignment while we were waiting. If so, skip.
        TourListing fresh = listingRepo.findById(listing.getId()).orElseThrow();
        if (fresh.getColorIndex() != null) return;

        double latDelta = (double) Constraints.MeetGraphicConstraints.PROXIMITY_METERS / 111_000.0;
        double lngDelta = latDelta / Math.cos(Math.toRadians(fresh.getLatitude()));

        List<TourListingRepo.MeetGraphicProjection> nearby = listingRepo.findNearbyAssignedGraphics(
                fresh.getId(),
                fresh.getMeetingDate().minusHours(Constraints.MeetGraphicConstraints.TIME_WINDOW_HOURS),
                fresh.getMeetingDate().plusHours(Constraints.MeetGraphicConstraints.TIME_WINDOW_HOURS),
                fresh.getLatitude() - latDelta,
                fresh.getLatitude() + latDelta,
                fresh.getLongitude() - lngDelta,
                fresh.getLongitude() + lngDelta
        );

        Set<Integer> usedEncoded = nearby.stream()
                .map(p -> p.getColorIndex() * Constraints.MeetGraphicConstraints.SYMBOL_COUNT + p.getSymbolIndex())
                .collect(Collectors.toSet());

        int[] pair = firstFreePair(usedEncoded, fresh.getId());
        fresh.assignMeetGraphic(pair[0], pair[1]);
        listingRepo.save(fresh);

        log.info("Assigned meet graphic (color={}, symbol={}) to listing {}", pair[0], pair[1], fresh.getId());
    }

    /**
     * Finds the first {@code (color, symbol)} pair not present in {@code usedEncoded},
     * starting from a seed derived from {@code listingId} to spread colours across listings.
     *
     * <p>Falls back to the seed pair itself if all 600 slots are occupied — which would require
     * 600 distinct tours within 300 m and 3 hours, a practically impossible scenario.
     *
     * @param usedEncoded encoded pairs already taken by nearby listings
     * @param listingId   seed source; different IDs start the search at different offsets
     * @return {@code [colorIndex, symbolIndex]}
     */
    static int[] firstFreePair(Set<Integer> usedEncoded, int listingId) {
        int seed = listingId % TOTAL_PAIRS;
        for (int i = 0; i < TOTAL_PAIRS; i++) {
            int candidate = (seed + i) % TOTAL_PAIRS;
            if (!usedEncoded.contains(candidate)) {
                return new int[]{
                        candidate / Constraints.MeetGraphicConstraints.SYMBOL_COUNT,
                        candidate % Constraints.MeetGraphicConstraints.SYMBOL_COUNT
                };
            }
        }
        return new int[]{
                seed / Constraints.MeetGraphicConstraints.SYMBOL_COUNT,
                seed % Constraints.MeetGraphicConstraints.SYMBOL_COUNT
        };
    }

    private void acquireAdvisoryLock(long key) {
        jdbcTemplate.execute(
                "SELECT pg_advisory_xact_lock(?)",
                (PreparedStatementCallback<Void>) ps -> {
                    ps.setLong(1, key);
                    ps.execute();
                    return null;
                }
        );
    }

    /**
     * Maps a coordinate to a ~1° geo-cell lock key.
     * Two listings in the same ~111 km cell get the same key and serialise their assignments.
     * Listings in different cells (e.g. different cities) get distinct keys and run in parallel.
     */
    private static long cellLockKey(double lat, double lng) {
        int cellLat = (int) Math.floor(lat);
        int cellLng = (int) Math.floor(lng);
        // Shift to non-negative: cellLat+90 in [0,180], cellLng+180 in [0,360]
        return (long) (cellLat + 90) * 361 + (cellLng + 180);
    }
}
