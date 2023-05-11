package ca.waaw.web.rest.errors.exceptions.application;

import lombok.Getter;

@Getter
public class ShiftNotAssignedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String action;

    public ShiftNotAssignedException(String action) {
        super();
        this.action = action;
    }
}
