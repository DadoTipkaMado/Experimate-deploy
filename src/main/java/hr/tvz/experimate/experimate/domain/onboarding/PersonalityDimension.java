package hr.tvz.experimate.experimate.domain.onboarding;

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

    /**
     * Converts display name into enum type.
     */
    public static PersonalityDimension fromDisplayname(String name) {
        String nameLowerCase = name.toLowerCase();
        return switch (nameLowerCase) {
            case "curiosity" -> OPENNESS;
            case "focus" -> CONSCIENTIOUSNESS;
            case "energy" -> EXTRAVERSION;
            case "warmth" -> AGREEABLENESS;
            case "sensitivity" -> NEUROTICISM;
            default -> null;
        };
    }
}
