package hr.tvz.experimate.experimate.shared.event;

import java.time.LocalDate;

public record GoogleUserCreationEvent(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String idNumber,
        String email,
        String username,
        String password,
        String googleSub
) {}
