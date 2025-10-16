package imagelify.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlanUpgradeException.class)
    public ResponseEntity<Object> handlePlanUpgradeException(PlanUpgradeException ex, WebRequest request) {
        return new ResponseEntity<>(createErrorResponse(ex.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(ImageLimitExceededException.class)
    public ResponseEntity<Object> handleImageLimitExceededException(ImageLimitExceededException ex, WebRequest request) {
        return new ResponseEntity<>(createErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> createErrorResponse(String message, HttpStatus status) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
