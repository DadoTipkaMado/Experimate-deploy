package hr.tvz.experimate.experimate.model.refresh_token;

import hr.tvz.experimate.experimate.model.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String token;

    private LocalDateTime expirationDateTime;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    //For hibernate
    protected RefreshToken() {}

    public RefreshToken(String token,
                        User user,
                        LocalDateTime expirationDateTime) {
        this.token = validateToken(token);
        this.user = validateUser(user);
        this.expirationDateTime = validateExpirationDateTime(expirationDateTime);
    }

    public Integer getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpirationDateTime() {
        return expirationDateTime;
    }

    public User getUser() {
        return user;
    }

    public void updateToken(String token, LocalDateTime expiration) {

    }

    private LocalDateTime validateExpirationDateTime(LocalDateTime expirationDateTime) {
        if (expirationDateTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Expiration Date cannot be before current date and time.");

        return expirationDateTime;
    }

    private User validateUser(User user) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null.");
        return user;
    }

    private String validateToken(String token) {
        if (token == null)
            throw new IllegalArgumentException("Token cannot be null.");
        return token;
    }
}
