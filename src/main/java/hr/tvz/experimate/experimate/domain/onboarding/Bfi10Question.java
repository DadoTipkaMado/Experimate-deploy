package hr.tvz.experimate.experimate.domain.onboarding;

/**
 * The ten items of the BFI-10 (Big Five Inventory – 10 item version) by Rammstedt & John (2007).
 *
 * <p>Each constant holds the item number (1–10), the exact verbatim statement shown to the user,
 * the {@link PersonalityDimension} it measures, and whether the item is
 * {@link Keying#NORMAL normally} or {@link Keying#REVERSE reverse}-keyed.
 *
 * <p>Do <strong>not</strong> rewrite the item text — the validity of the instrument depends on
 * exact wording from the original publication.
 */
public enum Bfi10Question {

    Q1 (1,  "is reserved",                    PersonalityDimension.EXTRAVERSION,      Keying.REVERSE),
    Q2 (2,  "is generally trusting",           PersonalityDimension.AGREEABLENESS,     Keying.NORMAL),
    Q3 (3,  "tends to be lazy",               PersonalityDimension.CONSCIENTIOUSNESS, Keying.REVERSE),
    Q4 (4,  "is relaxed, handles stress well", PersonalityDimension.NEUROTICISM,       Keying.REVERSE),
    Q5 (5,  "has few artistic interests",      PersonalityDimension.OPENNESS,          Keying.REVERSE),
    Q6 (6,  "is outgoing, sociable",           PersonalityDimension.EXTRAVERSION,      Keying.NORMAL),
    Q7 (7,  "tends to find fault with others", PersonalityDimension.AGREEABLENESS,     Keying.REVERSE),
    Q8 (8,  "does a thorough job",             PersonalityDimension.CONSCIENTIOUSNESS, Keying.NORMAL),
    Q9 (9,  "gets nervous easily",             PersonalityDimension.NEUROTICISM,       Keying.NORMAL),
    Q10(10, "has an active imagination",       PersonalityDimension.OPENNESS,          Keying.NORMAL);

    private final int itemNumber;
    private final String text;
    private final PersonalityDimension dimension;
    private final Keying keying;

    Bfi10Question(int itemNumber, String text, PersonalityDimension dimension, Keying keying) {
        this.itemNumber = itemNumber;
        this.text = text;
        this.dimension = dimension;
        this.keying = keying;
    }

    /**
     * Returns the 1-based item number matching the position in the original BFI-10 instrument.
     */
    public int getItemNumber() {
        return itemNumber;
    }

    /**
     * Returns the verbatim statement shown to the user (without the "I see myself as someone who…" prefix).
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the Big Five dimension this item contributes to.
     */
    public PersonalityDimension getDimension() {
        return dimension;
    }

    /**
     * Returns whether this item is scored normally or in reverse.
     * Used by {@link hr.tvz.experimate.experimate.domain.onboarding.Big5Calculator} during scoring.
     */
    public Keying getKeying() {
        return keying;
    }
}
