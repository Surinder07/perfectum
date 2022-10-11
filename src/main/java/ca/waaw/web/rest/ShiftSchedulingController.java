package ca.waaw.web.rest;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.dto.shifts.NewShiftBatchDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.service.ShiftSchedulingService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.shift-management}")
public class ShiftSchedulingController {

    private final ShiftSchedulingService shiftSchedulingService;

    private final AppRegexConfig appRegexConfig;

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
    public void assignShift(@RequestParam String shiftId, @RequestParam String userId) {
        shiftSchedulingService.assignShift(shiftId, userId);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.releaseShift}")
    @PutMapping("${api.endpoints.shift-management.releaseShift}")
    public void releaseShift(@RequestParam String shiftId) {
        shiftSchedulingService.releaseShift(shiftId);
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllShifts}")
    @GetMapping("${api.endpoints.shift-management.getAllShifts}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = ShiftDetailsDto.class)))})
    public ResponseEntity<List<ShiftDetailsDto>> getAllShifts(@Parameter(description = "${api.swagger.param-description.getShift-batchId}")
                                                              @RequestParam(required = false) String batchId,
                                                              @RequestParam String date,
                                                              @Parameter(description = "${api.swagger.param-description.getShift-endDate}")
                                                              @RequestParam(required = false) String endDate) {
        List<String> field = new ArrayList<>();
        if (!Pattern.matches(appRegexConfig.getDate(), date)) field.add("date");
        if (StringUtils.isNotEmpty(endDate) && !Pattern.matches(appRegexConfig.getDate(), endDate))
            field.add("endDate");
        if (!field.isEmpty()) {
            throw new BadRequestException("Invalid value", field.toArray(new String[0]));
        }
        return ResponseEntity.ok(shiftSchedulingService.getAllShifts(batchId, date, endDate));
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
    public ResponseEntity<ApiResponseMessageDto> releaseShiftsBatch(@RequestParam String batchId) {
        return ResponseEntity.ok(shiftSchedulingService.releaseShiftBatch(batchId));
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllShiftsBatch}")
    @GetMapping("${api.endpoints.shift-management.getAllShiftsBatch}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = BatchDetailsDto.class)))})
    public ResponseEntity<List<BatchDetailsDto>> getAllShiftsBatch() {
        return ResponseEntity.ok(shiftSchedulingService.getAllBatchDetails());
    }

}