package ca.waaw.web.rest;

import ca.waaw.web.rest.service.DashboardService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.dashboard}")
public class DashboardController {

    private DashboardService dashboardService;

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.dashboard.getData}")
    @GetMapping("${api.endpoints.dashboard.getData}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json")})
    public ResponseEntity<Map<String, Object>> getAllNotifications() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

}