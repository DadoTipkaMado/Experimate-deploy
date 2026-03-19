package hr.tvz.experimate.experimate.model.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name="app_user")
public class User extends Person {

    private String username;
    //TODO hashiraj password
    private String password;
    private String bio;
    private double rating = 0.0;

    //For Hibernate to operate
    protected User(){}

    private User(UserBuilder builder) {
        super(
                builder.firstName,
                builder.lastName,
                builder.dateOfBirth,
                builder.idNumber
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

        //user part (mandatory)
        private final String username;
        private final String password;
        //          (optional)
        private String bio;


        public UserBuilder(String firstName,
                            String lastName,
                            LocalDate dateOfBirth,
                            String idNumber,
                            String username,
                            String password) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.dateOfBirth = dateOfBirth;
            this.idNumber = idNumber;
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

    public void setPassword(String password) {
        this.password = password;
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

    private String validateUsername(String username) {
        if(username==null || username.isEmpty())
            throw new  IllegalArgumentException("Username cannot be empty");
        if(username.length() < 4 || username.length() > 15)
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