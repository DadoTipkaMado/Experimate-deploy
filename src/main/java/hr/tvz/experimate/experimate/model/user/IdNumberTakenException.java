package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.ConflictException;

public class IdNumberTakenException extends ConflictException {
    public IdNumberTakenException(String idNumber) {
        super("Id number: '%s' is already taken!".formatted(idNumber));
    }
}
