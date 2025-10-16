package imagelify.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
public class PlanUpgradeException extends RuntimeException {
    public PlanUpgradeException(String message) {
        super(message);
    }
}
