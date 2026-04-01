package hr.tvz.experimate.experimate.model.user;

import java.time.LocalDate;

public record CreateUserDto(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String idNumber,
        String username,
        //TODO hashiraj password
        String password,
        String bio
) {}
