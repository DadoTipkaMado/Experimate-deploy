package hr.tvz.experimate.experimate.user.exception;

import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;

public class IdNumberTakenException extends ConflictException {
    public IdNumberTakenException(String idNumber) {
        super("Id number: '%s' is already taken!".formatted(idNumber));
    }
}
