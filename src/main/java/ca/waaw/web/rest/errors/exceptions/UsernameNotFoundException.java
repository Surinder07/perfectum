package ca.waaw.web.rest.errors.exceptions;

public class UsernameNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UsernameNotFoundException(String loginName) {
        super(loginName);
    }

}
