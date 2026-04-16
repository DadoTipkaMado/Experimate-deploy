package hr.tvz.experimate.experimate.model.shared;
import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;
import hr.tvz.experimate.experimate.model.shared.exception.NotFoundException;
import hr.tvz.experimate.experimate.model.shared.exception.RefreshTokenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //UserNotFound, TourListingNotFound, ....
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        ErrorResponse notFound = createErrorResponse(HttpStatus.NOT_FOUND, ex);
        return new ResponseEntity<>(notFound, HttpStatus.NOT_FOUND);
    }

    //UsernameTaken, IdNumberTaken, TourListingAlreadyReserved, ...
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        ErrorResponse conflict = createErrorResponse(HttpStatus.CONFLICT, ex);
        return new ResponseEntity<>(conflict, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse illegalArg = createErrorResponse(HttpStatus.BAD_REQUEST, ex);
        return new ResponseEntity<>(illegalArg, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse illegalState = createErrorResponse(HttpStatus.CONFLICT, ex);
        return new ResponseEntity<>(illegalState, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        ErrorResponse authentication = createErrorResponse(HttpStatus.UNAUTHORIZED, ex);
        return new ResponseEntity<>(authentication, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleAppAuthException(RefreshTokenException ex) {
        ErrorResponse auth = createErrorResponse(HttpStatus.FORBIDDEN, ex);
        return new ResponseEntity<>(auth, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMapResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));
        ErrorMapResponse argumentNotValid = createErrorMapResponse(HttpStatus.BAD_REQUEST, errors);
        return new ResponseEntity<>(argumentNotValid, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMapResponse> handleConstraintViolationException(ConstraintViolationException ex){
        Map<Path, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        ConstraintViolation::getPropertyPath,
                        ConstraintViolation::getMessage
                ));
        ErrorMapResponse constraintViolation = createErrorMapResponse(HttpStatus.BAD_REQUEST, errors);
        return new ResponseEntity<>(constraintViolation, HttpStatus.BAD_REQUEST);
    }

    private ErrorResponse createErrorResponse(HttpStatus status, Exception ex) {
        return new ErrorResponse(
                status.value(),
                ex.getMessage(),
                LocalDateTime.now());
    }

    private ErrorMapResponse createErrorMapResponse(HttpStatus status, Map<?, ?> errors) {
        return new ErrorMapResponse(
                status.value(),
                errors,
                LocalDateTime.now()
        );
    }

    private record ErrorResponse(int status,
                                String message,
                                LocalDateTime timestamp) {}

    private record ErrorMapResponse(int status,
                                    Map<?, ?> errors,
                                    LocalDateTime timestamp) {}
}
