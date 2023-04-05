package ca.waaw.web.rest;

import ca.waaw.web.rest.service.WaawOpenService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerNotFound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.open-apis}")
public class WaawOpenController {

    private final WaawOpenService waawOpenService;

    @SwaggerNotFound
    @SwaggerBadRequest
    @Operation(description = "${api.description.open-apis.subscribe}")
    @GetMapping("${api.endpoints.open-apis.subscribe}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json")})
    public void subscribeToWaaw(@RequestParam String email) {
        waawOpenService.subscribeEmail(email);
    }

}