package hr.tvz.experimate.experimate.domain.user;

import hr.tvz.experimate.experimate.domain.onboarding.Big5Vector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application user entity, extending {@link Person} with account credentials,
 * profile data, and computed Big Five personality scores.
 *
 * <p>Personality fields are initially {@code null} and are populated by
 * {@link #setPersonalityMetricsFromVector(Big5Vector)} once the user completes
 * the onboarding quiz. {@code onboardingCompleted} is the canonical flag for
 * whether a personality profile exists; {@code personalityComputedAt} records when it was set.
 *
 * <p>Constructed exclusively via {@link UserBuilder}.
 */
@Entity
@Table(name="app_user")
public class User extends Person {

    private static final int MINIMUM_USERNAME_LENGTH = 4;
    private static final int MAXIMUM_USERNAME_LENGTH = 15;

    private String username;
    private String password;
    private String bio;
    @Column(unique = true)
    private String googleSub;
    private double rating = 0.0;
    private String profilePhotoFilename;

    private Double personalityOpenness;
    private Double personalityConscientiousness;
    private Double personalityExtraversion;
    private Double personalityAgreeableness;
    private Double personalityNeuroticism;
    private LocalDateTime personalityComputedAt;
    @Column(columnDefinition = "TEXT")
    private String personalitySummary;
    @Column(columnDefinition="boolean default false")
    private boolean onboardingCompleted = false;
    @Column(columnDefinition="int default 0")
    private int quizCompletionCount = 0;

    //For Hibernate to operate
    protected User(){}

    private User(UserBuilder builder) {
        super(
                builder.firstName,
                builder.lastName,
                builder.dateOfBirth,
                builder.idNumber,
                builder.email
        );
        this.username = validateUsername(builder.username);
        this.password = validatePassword(builder.password);
        this.bio = validateBio(builder.bio);
    }

    public static class UserBuilder {
        //Person part (mandatory)
        private final String firstName;
        private final String lastName;
        private final LocalDate dateOfBirth;
        private final String idNumber;
        private final String email;

        //user part (mandatory)
        private final String username;
        private final String password;
        //          (optional)
        private String bio;


        public UserBuilder(String firstName,
                            String lastName,
                            LocalDate dateOfBirth,
                            String idNumber,
                            String email,
                            String username,
                            String password) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.dateOfBirth = dateOfBirth;
            this.idNumber = idNumber;
            this.email = email;
            this.username = username;
            this.password = password;
        }

        public UserBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getProfilePhotoFilename() {
        return profilePhotoFilename;
    }

    public void setProfilePhotoFilename(String profilePhotoFilename) {
        this.profilePhotoFilename = profilePhotoFilename;
    }

    /**
     * Populates all five Big Five personality fields from the given vector, records the
     * timestamp, and marks onboarding as completed.
     *
     * <p>This is the only way personality data is written to the entity — there are no
     * individual setters for the personality fields.
     *
     * @param vector the computed {@link Big5Vector} returned by
     *               {@link hr.tvz.experimate.experimate.domain.onboarding.Big5Calculator#compute}
     */
    public void setPersonalityMetricsFromVector(Big5Vector vector) {
        this.personalityOpenness          = vector.openness();
        this.personalityConscientiousness = vector.conscientiousness();
        this.personalityExtraversion      = vector.extraversion();
        this.personalityAgreeableness     = vector.agreeableness();
        this.personalityNeuroticism       = vector.neuroticism();
        this.personalityComputedAt        = LocalDateTime.now();
        this.onboardingCompleted          = true;
    }

    /** @return {@code true} if the user has completed the onboarding quiz and has a personality profile. */
    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    /** @return Openness score in {@code [-1.0, +1.0]}, or {@code null} before onboarding. */
    public Double getPersonalityOpenness() { return personalityOpenness; }

    /** @return Conscientiousness score in {@code [-1.0, +1.0]}, or {@code null} before onboarding. */
    public Double getPersonalityConscientiousness() { return personalityConscientiousness; }

    /** @return Extraversion score in {@code [-1.0, +1.0]}, or {@code null} before onboarding. */
    public Double getPersonalityExtraversion() { return personalityExtraversion; }

    /** @return Agreeableness score in {@code [-1.0, +1.0]}, or {@code null} before onboarding. */
    public Double getPersonalityAgreeableness() { return personalityAgreeableness; }

    /** @return Neuroticism score in {@code [-1.0, +1.0]}, or {@code null} before onboarding. */
    public Double getPersonalityNeuroticism() { return personalityNeuroticism; }

    /** @return Timestamp of when the personality profile was last computed, or {@code null} before onboarding. */
    public LocalDateTime getPersonalityComputedAt() { return personalityComputedAt; }

    /** @return AI-generated natural-language summary of the personality profile, or {@code null} if not yet generated. */
    public String getPersonalitySummary() { return personalitySummary; }

    /** Sets the AI-generated personality summary. Called by {@link hr.tvz.experimate.experimate.domain.onboarding.OnboardingService} after quiz completion. */
    public void setPersonalitySummary(String personalitySummary) { this.personalitySummary = personalitySummary; }

    /** @return the number of times this user has completed the onboarding quiz. */
    public int getQuizCompletionCount() { return quizCompletionCount; }

    /**
     * Increments the quiz completion counter by one.
     *
     * <p>Called by the service layer each time the user successfully submits quiz answers.
     * The counter is never decremented — it persists across GDPR data deletions to
     * enforce the retake limit.
     */
    public void incrementQuizCompletionCount() {
        this.quizCompletionCount++;
    }

    /**
     * Clears all personality data from this user, reverting their onboarding state.
     *
     * <p>Called during a GDPR data deletion request. The quiz completion counter is
     * intentionally preserved to enforce the retake limit.
     */
    public void clearPersonalityData() {
        this.personalityOpenness          = null;
        this.personalityConscientiousness = null;
        this.personalityExtraversion      = null;
        this.personalityAgreeableness     = null;
        this.personalityNeuroticism       = null;
        this.personalityComputedAt        = null;
        this.personalitySummary           = null;
        this.onboardingCompleted          = false;
    }

    private String validateUsername(String username) {
        if(username==null || username.isEmpty())
            throw new  IllegalArgumentException("Username cannot be empty");
        if(username.length() < MINIMUM_USERNAME_LENGTH || username.length() > MAXIMUM_USERNAME_LENGTH)
            throw new IllegalArgumentException(
                    "Invalid username: '%s', must be 4 to 15 characters long"
                    .formatted(username));
        return username;
    }

    private String validatePassword(String password) {
        if(password==null || password.isEmpty())
            throw new  IllegalArgumentException("Password cannot be empty");
        return password;
    }

    private String validateBio(String bio) {
        if(bio != null && bio.length() > 200)
            throw new IllegalArgumentException("Bio cannot be longer than 200 characters");
        return bio;
    }
}