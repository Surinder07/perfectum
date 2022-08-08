package ca.waaw.web.rest.errors.exceptions;

public class ExpiredKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExpiredKeyException(String keyType) {
        super(keyType);
    }

}
