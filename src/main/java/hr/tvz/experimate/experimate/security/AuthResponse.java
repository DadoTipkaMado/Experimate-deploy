package hr.tvz.experimate.experimate.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(@JsonProperty("token") String jwt) {
}
