package ca.waaw.web.rest.errors.exceptions.application;

import ca.waaw.enumration.user.Authority;
import lombok.Getter;

@Getter
public class PaymentPendingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Authority role;

    public PaymentPendingException(Authority role) {
        super();
        this.role = role;
    }

}
