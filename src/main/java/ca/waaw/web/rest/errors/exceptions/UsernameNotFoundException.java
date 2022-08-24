package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class UsernameNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String loginName;

    public UsernameNotFoundException(String loginName) {
        super();
        this.loginName = loginName;
    }

}
