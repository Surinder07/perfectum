package ca.waaw.web.rest.errors;

import ca.waaw.enumration.Authority;
import lombok.*;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PaymentErrorVM extends ErrorVM {

    private final Authority role;

    private final String userId;

    public PaymentErrorVM(String message, Authority role, String userId, String... fields) {
        super(message, fields);
        this.role = role;
        this.userId = userId;
    }

}
