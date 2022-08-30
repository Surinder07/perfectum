package ca.waaw.web.rest.utils.customannotations.swagger;

import ca.waaw.web.rest.errors.ErrorVM;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add to any api to show jwt authorization on swagger
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SecurityRequirement(name = "jwt")
@ApiResponse(responseCode = "401", description = "${api.swagger.error-description.authentication}", content = @Content)
@ApiResponse(responseCode = "402", description = "${api.swagger.error-description.trial-over}",
        content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorVM.class))})
public @interface SwaggerAuthenticated {
}
