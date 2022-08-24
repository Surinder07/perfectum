package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class UserNotActivatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String loginName;

    public UserNotActivatedException(String loginName) {
        super();
        this.loginName = loginName;
    }

}
