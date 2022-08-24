package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String[] fields;

    public BadRequestException(String message, String... fields) {
        super(message);
        this.fields = fields;
    }

}
