package ca.waaw.web.rest;

import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.dto.shifts.NewShiftBatchDto;
import ca.waaw.dto.shifts.NewShiftDto;
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

import javax.validation.Valid;

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
    public void createShift(@Valid @RequestBody NewShiftDto newShiftDto) {
        shiftSchedulingService.createShift(newShiftDto);
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
    public void deleteShift(@RequestParam String shiftId) {
        shiftSchedulingService.deleteShift(shiftId);
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

    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", description = "Success. Show the response message to user.",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseMessageDto.class))})
    @Operation(description = "${api.description.shift-management.claimShift}")
    @PutMapping("${api.endpoints.shift-management.claimShift}")
    public void claimShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.approveShift}")
    @PutMapping("${api.endpoints.shift-management.approveShift}")
    public void approveClaimedShift() {
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.releaseShift}")
    @PutMapping("${api.endpoints.shift-management.releaseShift}")
    public void releaseShift() {
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

    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @SwaggerRespondWithMessage
    @Operation(description = "${api.description.shift-management.createShiftsBatch}")
    @PostMapping("${api.endpoints.shift-management.createShiftsBatch}")
    public ResponseEntity<ApiResponseMessageDto> createShiftsBatch(@Valid @RequestBody NewShiftBatchDto newShiftBatchDto) {
        return ResponseEntity.ok(shiftSchedulingService.createNewBatch(newShiftBatchDto));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.releaseShiftsBatch}")
    @PutMapping("${api.endpoints.shift-management.releaseShiftsBatch}")
    public void releaseShiftsBatch() {
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllShiftsBatch}")
    @GetMapping("${api.endpoints.shift-management.getAllShiftsBatch}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = AdminLocationDto.class))}, description = "${api.swagger.schema-description.getAllShifts}")
    public ResponseEntity<Object> getAllShiftsBatch(@RequestParam(required = false) String locationId,
                                                    @RequestParam(required = false) String location_role_id,
                                                    @PathVariable int pageNo, @PathVariable int pageSize) {
        return null;
    }

}