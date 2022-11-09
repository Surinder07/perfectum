package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.web.rest.utils.customannotations.helperclass.ValueOfEnumValidator;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Add to any Dto field to check if the string value is within an enum values
 * usage: @ValueOfEnum(enumClass = your_enum.class)
 */
@SuppressWarnings("unused")
@Documented
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = ValueOfEnumValidator.class)
@JacksonAnnotationsInside
public @interface ValueOfEnum {
    Class<? extends Enum<?>> enumClass();

    String message() default "invalid value; must be any of enum {enumClass}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
