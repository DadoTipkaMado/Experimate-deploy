package hr.tvz.experimate.experimate.shared.event;

public record UserRegisteredEvent(Integer userId, String email, String firstName) {}
