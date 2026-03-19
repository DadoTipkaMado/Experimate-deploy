package hr.tvz.experimate.experimate.model.shared;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
