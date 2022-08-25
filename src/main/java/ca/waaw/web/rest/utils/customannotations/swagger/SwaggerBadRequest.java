package ca.waaw.web.rest.utils.customannotations.swagger;

import ca.waaw.web.rest.errors.ErrorVM;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add to any controller method to show on swagger that it may throw this error
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "400", description = "${api.swagger.error-description.bad-request}",
        content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorVM.class))})
public @interface SwaggerBadRequest {
}
