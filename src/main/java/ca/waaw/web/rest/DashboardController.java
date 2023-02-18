package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.web.rest.service.DashboardService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.dashboard.getShiftData}")
    @GetMapping("${api.endpoints.dashboard.getShiftData}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = ShiftDetailsDto.class)))})
    public ResponseEntity<PaginationDto> getShiftData(@PathVariable int pageNo, @PathVariable int pageSize) {
        return ResponseEntity.ok(dashboardService.getShiftData(pageNo, pageSize));
    }

}