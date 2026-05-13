package hr.tvz.experimate.experimate.domain.user.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class IdNumberTakenException extends ConflictException {
    public IdNumberTakenException(String idNumber) {
        super("Id number: '%s' is already taken!".formatted(idNumber));
    }
}
