package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.web.rest.utils.customannotations.helperclass.FirstAlphabetCapitalDeserializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Capitalize first letter for any String Dto field
 * Example - test user becomes Test User
 */
@Documented
@Target({FIELD})
@Retention(RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = FirstAlphabetCapitalDeserializer.class)
public @interface CapitalizeFirstLetter {
}
