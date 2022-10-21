package ca.waaw.web.rest.errors.exceptions.application;

import lombok.Getter;

@Getter
public class MissingRequiredFieldsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fileType;

    private final String[] field;

    public MissingRequiredFieldsException(String fileType, String[] field) {
        super();
        this.fileType = fileType;
        this.field = field;
    }
}
