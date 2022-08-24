package ca.waaw.web.rest.errors.exceptions;

import ca.waaw.enumration.Authority;
import lombok.Getter;

@Getter
public class TrialExpiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String userId;

    private final Authority role;

    public TrialExpiredException(String userId, Authority role) {
        super();
        this.userId = userId;
        this.role = role;
    }

}
