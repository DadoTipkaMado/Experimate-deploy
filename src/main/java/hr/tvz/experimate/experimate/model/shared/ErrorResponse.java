package hr.tvz.experimate.experimate.model.shared;

import java.time.LocalDateTime;

public record ErrorResponse(int status,
                            String message,
                            LocalDateTime timestamp) {
}
