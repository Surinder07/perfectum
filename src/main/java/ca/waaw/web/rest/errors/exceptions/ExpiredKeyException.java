package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class ExpiredKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String keyType;

    public ExpiredKeyException(String keyType) {
        super();
        this.keyType = keyType;
    }

}
