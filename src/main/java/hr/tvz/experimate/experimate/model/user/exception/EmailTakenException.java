package hr.tvz.experimate.experimate.domain.user.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class EmailTakenException extends ConflictException {
    public EmailTakenException(String email) {
        super("Email: '%s' is already taken!".formatted(email));
    }
}
