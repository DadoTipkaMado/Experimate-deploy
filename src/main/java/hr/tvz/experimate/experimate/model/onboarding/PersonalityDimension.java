package hr.tvz.experimate.experimate.model.onboarding;

/**
 * The five dimensions of the Big Five personality model (OCEAN).
 * Each dimension has a user-facing display name used in the Stat Sheet UI.
 */
public enum PersonalityDimension {
    OPENNESS("Curiosity"),
    CONSCIENTIOUSNESS("Focus"),
    EXTRAVERSION("Energy"),
    AGREEABLENESS("Warmth"),
    NEUROTICISM("Sensitivity");

    private final String displayName;

    PersonalityDimension(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the user-facing label shown in the Stat Sheet (e.g. "Curiosity" for Openness).
     */
    public String getDisplayName() {
        return displayName;
    }
}
