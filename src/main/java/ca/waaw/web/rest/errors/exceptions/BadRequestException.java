package ca.waaw.web.rest.errors.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String[] fields;

    public BadRequestException(String message, String... fields) {
        super(message);
        this.fields = fields;
    }

}
