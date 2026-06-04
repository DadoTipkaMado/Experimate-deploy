package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.shared.Constraints;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MeetGraphicAssignerTest {

    private static final int COLORS  = Constraints.MeetGraphicConstraints.COLOR_COUNT;
    private static final int SYMBOLS = Constraints.MeetGraphicConstraints.SYMBOL_COUNT;
    private static final int TOTAL   = COLORS * SYMBOLS;

    // ──────────────── firstFreePair ────────────────

    @Test
    void firstFreePair_whenSeedOccupied_returnsNextCandidate() {
        int listingId = 10;
        int seed = listingId % TOTAL;

        int[] pair = MeetGraphicAssigner.firstFreePair(Set.of(seed), listingId);
        int encoded = pair[0] * SYMBOLS + pair[1];

        assertEquals((seed + 1) % TOTAL, encoded);
    }

    @Test
    void firstFreePair_whenMultipleOccupied_skipsAllAndReturnsFirstFree() {
        int listingId = 5;
        int seed = listingId % TOTAL;

        Set<Integer> used = IntStream.range(0, 10)
                .mapToObj(i -> (seed + i) % TOTAL)
                .collect(Collectors.toSet());

        int[] pair = MeetGraphicAssigner.firstFreePair(used, listingId);
        int encoded = pair[0] * SYMBOLS + pair[1];

        assertFalse(used.contains(encoded));
        assertEquals((seed + 10) % TOTAL, encoded);
    }

    @Test
    void firstFreePair_whenAllSlotsOccupied_fallsBackToSeed() {
        int listingId = 7;
        int seed = listingId % TOTAL;

        Set<Integer> allUsed = IntStream.range(0, TOTAL).boxed().collect(Collectors.toSet());

        int[] pair = MeetGraphicAssigner.firstFreePair(allUsed, listingId);
        int encoded = pair[0] * SYMBOLS + pair[1];

        assertEquals(seed, encoded);
    }

    @Test
    void firstFreePair_whenSeedNearEnd_wrapsAroundAndReturnsFirstFree() {
        int listingId = TOTAL - 1;          // seed = 599
        int seed = listingId % TOTAL;

        Set<Integer> used = new HashSet<>();
        used.add(seed);                     // 599 je zauzet

        int[] pair = MeetGraphicAssigner.firstFreePair(used, listingId);
        int encoded = pair[0] * SYMBOLS + pair[1];

        assertNotEquals(seed, encoded);
        assertEquals(0, encoded);           // (599 + 1) % 600 = 0
    }
}
