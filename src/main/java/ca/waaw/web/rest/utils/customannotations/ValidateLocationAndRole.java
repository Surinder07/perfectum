package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.enumration.Authority;
import ca.waaw.web.rest.utils.customannotations.helperclass.ValidateLocationAndRoleValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Used only for {@link InviteUserDto}
 * If role is {@link Authority#MANAGER} {@link InviteUserDto#getLocationId()} should not be null
 * If role is {@link Authority#EMPLOYEE} or {@link Authority#CONTRACTOR},
 * {@link InviteUserDto#getLocationId()} or {@link InviteUserDto#getLocationRoleId()}should not be null
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidateLocationAndRoleValidator.class)
public @interface ValidateLocationAndRole {

    String message() default "location_id is required for manager/ both location_id and location_role_id is required for a employee";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
