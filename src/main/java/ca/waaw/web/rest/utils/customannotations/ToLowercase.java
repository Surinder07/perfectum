package ca.waaw.web.rest.utils.customannotations;

import ca.waaw.web.rest.utils.customannotations.helperclass.ToLowerCaseDeserializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Add to any Dto field to convert it to lowercase
 */
@Documented
@Target({FIELD})
@Retention(RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = ToLowerCaseDeserializer.class)
public @interface ToLowercase {
}
