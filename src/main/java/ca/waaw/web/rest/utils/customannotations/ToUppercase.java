package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.web.rest.utils.customannotations.helperclass.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Add to any Dto field to convert it to uppercase
 */
@Documented
@Target({FIELD})
@Retention(RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = ToUpperCaseDeserializer.class)
public @interface ToUppercase {
}
