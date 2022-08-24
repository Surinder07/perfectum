package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.web.rest.utils.customannotations.helperclass.DependentDtoFieldsValidator;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation for some DTOs, where fields are dependent on each other
 * Check the {@link DependentDtoFieldsValidator class for different validations}
 */
@SuppressWarnings("unused")
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DependentDtoFieldsValidator.class)
public @interface ValidateDependentDtoField {

    DependentDtoFieldsValidatorType type();

    String message() default "Some required value is missing";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
