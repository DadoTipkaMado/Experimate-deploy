package hr.tvz.experimate.experimate.model.user;

public class IdNumberTakenException extends RuntimeException {
    public IdNumberTakenException(String idNumber) {
        super("Id number: '%s' is already taken!".formatted(idNumber));
    }
}
