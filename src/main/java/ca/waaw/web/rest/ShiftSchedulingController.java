package ca.waaw.web.rest;

import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.web.rest.service.ShiftSchedulingService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.shift-management}")
public class ShiftSchedulingController {

    private final ShiftSchedulingService shiftSchedulingService;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.shift-management.createShift}")
    @PostMapping("${api.endpoints.shift-management.createShift}")
    public void createShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.updateShift}")
    @PutMapping("${api.endpoints.shift-management.updateShift}")
    public void updateShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.deleteShift}")
    @DeleteMapping("${api.endpoints.shift-management.deleteShift}")
    public void deleteShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.assignShift}")
    @PutMapping("${api.endpoints.shift-management.assignShift}")
    public void assignShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.claimShift}")
    @PutMapping("${api.endpoints.shift-management.claimShift}")
    public void claimShift() {
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.shift-management.createRecurringShift}")
    @PostMapping("${api.endpoints.shift-management.createRecurringShift}")
    public void createRecurringShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.updateRecurringShift}")
    @PutMapping("${api.endpoints.shift-management.updateRecurringShift}")
    public void updateRecurringShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.deleteRecurringShift}")
    @DeleteMapping("${api.endpoints.shift-management.deleteRecurringShift}")
    public void deleteRecurringShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.assignRecurringShift}")
    @PutMapping("${api.endpoints.shift-management.assignRecurringShift}")
    public void assignRecurringShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.claimRecurringShift}")
    @PutMapping("${api.endpoints.shift-management.claimRecurringShift}")
    public void claimRecurringShift() {
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllShifts}")
    @GetMapping("${api.endpoints.shift-management.getAllShifts}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = AdminLocationDto.class))}, description = "${api.swagger.schema-description.getAllShifts}")
    public ResponseEntity<Object> getAllShifts(@RequestParam(required = false) String locationId,
                                               @RequestParam(required = false) String location_role_id,
                                               @RequestParam String date, @RequestParam(required = false) String endDate,
                                               @PathVariable int pageNo, @PathVariable int pageSize) {
        return null;
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllRecurringShifts}")
    @GetMapping("${api.endpoints.shift-management.getAllRecurringShifts}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = AdminLocationDto.class))}, description = "${api.swagger.schema-description.getAllShifts}")
    public ResponseEntity<Object> getAllRecurringShifts(@RequestParam(required = false) String locationId,
                                                        @RequestParam(required = false) String location_role_id,
                                                        @PathVariable int pageNo, @PathVariable int pageSize) {
        return null;
    }

}