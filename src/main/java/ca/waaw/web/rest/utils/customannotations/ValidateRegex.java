package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.web.rest.utils.customannotations.helperclass.RegexMatcherValidator;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.ValidatorType;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Will check any dto field for given regex patterns, if anyone matches it will allow process to continue
 * Pass one of the {@link ValidatorType}
 */
@SuppressWarnings("unused")
@Documented
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = RegexMatcherValidator.class)
public @interface ValidateRegex {

    ValidatorType type();

    String message() default "invalid pattern";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
