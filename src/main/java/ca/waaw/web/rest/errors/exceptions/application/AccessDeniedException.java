package ca.waaw.web.rest.errors.exceptions.application;

import ca.waaw.enumration.ErrorCodes;
import lombok.Getter;

@Getter
public class AccessDeniedException extends RuntimeException {

    private final ErrorCodes errorCode;

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(ErrorCodes errorCode) {
        super();
        this.errorCode = errorCode;
    }

}
