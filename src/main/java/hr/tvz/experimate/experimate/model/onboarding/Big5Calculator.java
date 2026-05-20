package hr.tvz.experimate.experimate.model.onboarding;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-math service for all BFI-10 scoring and Big Five vector operations.
 *
 * <p>Contains no LLM calls, no I/O, and no side effects — only deterministic
 * calculations based on the BFI-10 instrument (Rammstedt & John, 2007) and the
 * weighted cosine similarity formula defined in the ExperiMate matching specification.
 */
@Service
public class Big5Calculator {

    /**
     * Per-dimension weights used in {@link #weightedCosineSimilarity}.
     *
     * <p>Extraversion and Agreeableness are weighted highest (1.3) because they are the
     * strongest predictors of friction in a 1-on-1 social context. Neuroticism is weighted
     * lowest (0.5) to avoid systematically segregating anxious users.
     */
    private static final Map<PersonalityDimension, Double> WEIGHTS = new EnumMap<>(Map.of(
            PersonalityDimension.OPENNESS,           1.2,
            PersonalityDimension.CONSCIENTIOUSNESS,  0.8,
            PersonalityDimension.EXTRAVERSION,       1.3,
            PersonalityDimension.AGREEABLENESS,      1.3,
            PersonalityDimension.NEUROTICISM,        0.5
    ));

    /**
     * Computes a {@link Big5Vector} from raw BFI-10 Likert responses.
     *
     * <p>For each of the five dimensions:
     * <ol>
     *   <li>Reverse-keyed items are flipped: {@code 6 - rawAnswer}.
     *       The formula {@code maxValue + 1} mirrors the answer around the scale midpoint —
     *       on a 1–5 scale this is {@code 5 + 1 = 6}, so answer 1 becomes 5 and vice versa.</li>
     *   <li>The two scored items are averaged: {@code raw = (item1 + item2) / 2.0}, giving a value in [1.0, 5.0].</li>
     *   <li>The average is normalised to [-1, +1]: {@code (raw - 3.0) / 2.0}.</li>
     * </ol>
     *
     * @param rawAnswers map of itemNumber (1–10) to Likert response (1–5)
     * @return the computed {@link Big5Vector}
     * @throws IllegalArgumentException if any of the 10 item numbers is missing from the map
     */
    public Big5Vector compute(Map<Integer, Integer> rawAnswers) {
        validateAnswers(rawAnswers);

        return new Big5Vector(
                computeDimension(PersonalityDimension.OPENNESS,           rawAnswers),
                computeDimension(PersonalityDimension.CONSCIENTIOUSNESS,  rawAnswers),
                computeDimension(PersonalityDimension.EXTRAVERSION,       rawAnswers),
                computeDimension(PersonalityDimension.AGREEABLENESS,      rawAnswers),
                computeDimension(PersonalityDimension.NEUROTICISM,        rawAnswers)
        );
    }

    /**
     * Computes a weighted cosine similarity score between two Big Five vectors.
     *
     * <p>Formula:
     * <pre>
     *   numerator   = Σ w[d] * v1[d] * v2[d]
     *   denominator = √(Σ w[d] * v1[d]²) × √(Σ w[d] * v2[d]²)
     *   similarity  = numerator / denominator          → [-1, +1]
     *   score       = round((similarity + 1) / 2 × 100) → [0, 100]
     * </pre>
     *
     * @param v1 first user's Big Five vector
     * @param v2 second user's Big Five vector
     * @return match score in [0, 100]; returns 50 if either vector has zero magnitude
     */
    public int weightedCosineSimilarity(Big5Vector v1, Big5Vector v2) {
        double[] vals1 = toArray(v1);
        double[] vals2 = toArray(v2);
        PersonalityDimension[] dims = PersonalityDimension.values();

        double numerator = 0.0;
        double sumSq1    = 0.0;
        double sumSq2    = 0.0;

        for (int i = 0; i < dims.length; i++) {
            double w   = WEIGHTS.get(dims[i]);
            numerator += w * vals1[i] * vals2[i];
            sumSq1    += w * vals1[i] * vals1[i];
            sumSq2    += w * vals2[i] * vals2[i];
        }

        double denominator = Math.sqrt(sumSq1) * Math.sqrt(sumSq2);
        if (denominator == 0.0) return 50;

        double similarity = numerator / denominator;
        return (int) Math.round((similarity + 1.0) / 2.0 * 100.0);
    }

    /**
     * Converts a Big Five dimension value from the internal [-1, +1] range to a
     * display-friendly [0, 100] integer for the Stat Sheet UI.
     *
     * <p>This method is used <em>only for display</em>. All matching logic operates
     * on the raw [-1, +1] values stored in {@link Big5Vector}.
     *
     * @param dimensionValue a value from {@link Big5Vector} in the range [-1.0, +1.0]
     * @return display value in [0, 100]
     */
    public int normalizeForDisplay(double dimensionValue) {
        return (int) Math.round((dimensionValue + 1.0) / 2.0 * 100.0);
    }

    // --- private helpers ---

    /**
     * Scores a single dimension by locating its two BFI-10 items, applying reverse-keying
     * where needed, averaging the two scored values, and normalising to [-1, +1].
     */
    private double computeDimension(PersonalityDimension dimension, Map<Integer, Integer> rawAnswers) {
        List<Bfi10Question> items = Arrays.stream(Bfi10Question.values())
                .filter(q -> q.getDimension() == dimension)
                .toList();

        double sum = 0.0;
        for (Bfi10Question item : items) {
            int answer = rawAnswers.get(item.getItemNumber());
            sum += (item.getKeying() == Keying.REVERSE) ? 6 - answer : answer;
        }

        double raw = sum / items.size();
        return (raw - 3.0) / 2.0;
    }

    /**
     * Validates that the answer map contains an entry for each of the 10 BFI-10 item numbers.
     *
     * @throws IllegalArgumentException if any item number is missing
     */
    private void validateAnswers(Map<Integer, Integer> rawAnswers) {
        for (Bfi10Question q : Bfi10Question.values()) {
            if (!rawAnswers.containsKey(q.getItemNumber())) {
                throw new IllegalArgumentException(
                        "Missing answer for BFI-10 item " + q.getItemNumber());
            }
        }
    }

    /**
     * Converts a {@link Big5Vector} to a {@code double[]} ordered by {@link PersonalityDimension#values()},
     * required for the index-aligned cosine similarity loop.
     */
    private double[] toArray(Big5Vector v) {
        return new double[]{
                v.openness(),
                v.conscientiousness(),
                v.extraversion(),
                v.agreeableness(),
                v.neuroticism()
        };
    }
}
