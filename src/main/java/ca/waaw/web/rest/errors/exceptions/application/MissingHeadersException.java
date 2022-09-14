package ca.waaw.web.rest.errors.exceptions.application;

import lombok.Getter;

@Getter
public class MissingHeadersException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fileType;

    private final String[] headers;

    public MissingHeadersException(String fileType, String[] headers) {
        super();
        this.fileType = fileType;
        this.headers = headers;
    }
}
