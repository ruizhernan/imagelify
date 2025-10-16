package imagelify.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ImageLimitExceededException extends RuntimeException {
    public ImageLimitExceededException(String message) {
        super(message);
    }
}
