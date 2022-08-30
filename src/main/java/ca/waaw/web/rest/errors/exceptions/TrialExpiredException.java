package ca.waaw.web.rest.errors.exceptions;

import ca.waaw.enumration.Authority;
import lombok.Getter;

@Getter
public class TrialExpiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Authority role;

    public TrialExpiredException(Authority role) {
        super();
        this.role = role;
    }

}
