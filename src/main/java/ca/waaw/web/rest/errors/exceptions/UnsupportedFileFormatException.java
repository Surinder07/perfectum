package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class UnsupportedFileFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String[] allowedFormats;

    public UnsupportedFileFormatException(String[] allowedFormats) {
        super();
        this.allowedFormats = allowedFormats;
    }

}
