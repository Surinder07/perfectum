package ca.waaw.web.rest.errors;

import ca.waaw.enumration.ErrorCodes;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

@Getter
public class ErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] fields;

    private ErrorCodes waawErrorCode = null;

    private final String message;

    private final Instant timestamp = Instant.now();

    public ErrorVM(String message, String... fields) {
        this.fields = fields;
        this.message = message;
    }

    public ErrorVM(ErrorCodes errorCode, String... fields) {
        this.fields = fields;
        this.waawErrorCode = errorCode;
        this.message = errorCode.value;
    }

}
