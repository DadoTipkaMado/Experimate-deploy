package hr.tvz.experimate.experimate.shared.util;

import java.util.Random;

/**
 * Produces a privacy-preserving, fuzzed view of a precise geographic location.
 *
 * <p>A tour listing's exact coordinates must never reach a viewer who has not yet
 * been approved by the host. Instead of the real point, the API returns a circle:
 * a deterministically offset center plus a display radius. The frontend draws the
 * circle so the viewer learns the rough neighbourhood, not the exact spot.
 *
 * <p><strong>Why the offset is deterministic.</strong> The center is always shifted
 * exactly {@value #OFFSET_METERS} m in a pseudo-random bearing seeded by the listing id.
 * The same listing therefore always yields the same fuzzed center. If the bearing were
 * re-randomized per request, a caller could average many responses and recover the true
 * location.
 *
 * <p><strong>Why the distance is fixed, not random.</strong> Using a random distance in
 * {@code [0, MAX_OFFSET]} risks a near-zero offset — the fuzzed center would sit almost
 * on the true point and the privacy guarantee collapses. A fixed distance of
 * {@value #OFFSET_METERS} m ensures every listing gets the same level of protection.
 * The real point always falls within the {@value #DISPLAY_RADIUS_METERS} m display
 * circle (because {@value #OFFSET_METERS} &lt; {@value #DISPLAY_RADIUS_METERS}), but
 * never at its center.
 */
public final class LocationObfuscator {

    /**
     * Meters per degree of latitude. Constant everywhere; longitude degrees are
     * narrower near the poles and are corrected separately with {@code cos(latitude)}.
     */
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

    /** Fixed distance the fuzzed center is shifted from the true point. */
    private static final int OFFSET_METERS = 300;

    /** Radius of the circle the frontend draws around the fuzzed center. */
    private static final int DISPLAY_RADIUS_METERS = 500;

    private LocationObfuscator() {
    }

    /**
     * Returns a fuzzed circle that hides the given precise coordinates.
     *
     * <p>The fuzzed center is placed exactly {@value #OFFSET_METERS} m from the true
     * point in a direction determined by {@code seed}. Meters are converted to degrees
     * with an equirectangular approximation — accurate to well under a meter at this
     * scale. Longitude is divided by {@code cos(latitude)} so the offset is isotropic
     * regardless of how far north or south the point lies.
     *
     * @param latitude  the true latitude in decimal degrees
     * @param longitude the true longitude in decimal degrees
     * @param seed      a stable per-location value (e.g. the listing id) that makes the
     *                  bearing deterministic across requests
     * @return the fuzzed center and the radius the frontend should display
     */
    public static ObfuscatedLocation obfuscate(double latitude, double longitude, long seed) {
        double bearing = new Random(seed).nextDouble() * 2 * Math.PI;

        double deltaLatitude = (OFFSET_METERS * Math.cos(bearing)) / METERS_PER_DEGREE_LATITUDE;
        double deltaLongitude = (OFFSET_METERS * Math.sin(bearing))
                / (METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(latitude)));

        return new ObfuscatedLocation(
                latitude + deltaLatitude,
                longitude + deltaLongitude,
                DISPLAY_RADIUS_METERS
        );
    }

    /**
     * A fuzzed location: the offset center the API exposes plus the radius the
     * frontend draws around it.
     */
    public record ObfuscatedLocation(double latitude, double longitude, int radiusMeters) {
    }
}
