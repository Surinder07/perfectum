package ca.waaw.web.rest.utils.customannotations.swagger;

import ca.waaw.dto.ApiResponseMessageDto;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add to any controller method to show on swagger that it will respond with a message
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "201", description = "${api.swagger.schema-description.response-message}",
        content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseMessageDto.class))})
public @interface SwaggerRespondWithMessage {
}
