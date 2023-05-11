package ca.waaw.web.rest;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.shifts.UpdateShiftDto;
import ca.waaw.enumration.shift.ShiftBatchStatus;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.web.rest.service.ShiftsService;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.shift-management}")
public class ShiftsController {

    private final Logger log = LogManager.getLogger(ShiftsController.class);

    private final ShiftsService shiftsService;

    private final AppRegexConfig appRegexConfig;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.shift-management.createShift}")
    @PostMapping("${api.endpoints.shift-management.createShift}")
    public ResponseEntity<ApiResponseMessageDto> createShift(@Valid @RequestBody NewShiftDto newShiftDto) {
        return ResponseEntity.ok(shiftsService.createShift(newShiftDto));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.editShift}")
    @PutMapping("${api.endpoints.shift-management.editShift}")
    public void updateShift(@Valid @RequestBody UpdateShiftDto updateShiftDto) {
        try {
            shiftsService.updateShift(updateShiftDto);
        } catch (Exception e) {
            log.error("Exception while updating shift ", e);
            throw e;
        }
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
        shiftsService.deleteShift(shiftId);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.deleteBatch}")
    @DeleteMapping("${api.endpoints.shift-management.deleteBatch}")
    public void deleteBatch(@RequestParam String batchId) {
        shiftsService.deleteBatch(batchId);
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
        shiftsService.releaseShift(shiftId);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.releaseBatch}")
    @PutMapping("${api.endpoints.shift-management.releaseBatch}")
    public void releaseBatch(@RequestParam String batchId) {
        shiftsService.releaseBatch(batchId);
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
        shiftsService.assignShift(shiftId, userId);
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllShifts}")
    @GetMapping("${api.endpoints.shift-management.getAllShifts}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = BatchDetailsDto.class)))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllShifts(@PathVariable int pageNo, @PathVariable int pageSize,
                                                      @RequestParam(required = false) String searchKey,
                                                      @RequestParam(required = false) String locationId,
                                                      @RequestParam(required = false) String roleId,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate,
                                                      @RequestParam(required = false) String batchStatus,
                                                      @RequestParam(required = false) String shiftStatus,
                                                      @RequestParam(required = false) String shiftType) {
        if (startDate != null && endDate != null) {
            CommonUtils.validateDateFormat(new String[]{startDate, "startDate"}, new String[]{endDate, "endDate"});
        }
        CommonUtils.validateStringInEnum(ShiftStatus.class, shiftStatus, "shiftStatus");
        CommonUtils.validateStringInEnum(ShiftBatchStatus.class, batchStatus, "batchStatus");
        if (shiftType == null) shiftType = "ALL";
        else CommonUtils.matchAndValidateStringValues(shiftType, "shiftType", "ALL", "UPCOMING", "TODAYS");
        try {
            return ResponseEntity.ok(shiftsService.getAllShifts(pageNo, pageSize, searchKey, locationId, roleId,
                    startDate, endDate, batchStatus, shiftStatus, shiftType));
        } catch (Exception e) {
            log.error("Exception while fetching all shifts: ", e);
            throw e;
        }
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getAllShiftsUser}")
    @GetMapping("${api.endpoints.shift-management.getAllShiftsUser}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = ShiftDetailsDto.class)))})
    public ResponseEntity<PaginationDto> getAllShiftsUser(@PathVariable int pageNo, @PathVariable int pageSize,
                                                          @RequestParam(required = false) String userId,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate,
                                                          @RequestParam(required = false) String shiftStatus,
                                                          @RequestParam(required = false) String searchKey) {
        if (startDate != null && endDate != null) {
            CommonUtils.validateDateFormat(new String[]{startDate, "startDate"}, new String[]{endDate, "endDate"});
        }
        CommonUtils.validateStringInEnum(ShiftStatus.class, shiftStatus, "shiftStatus");
        try {
            return ResponseEntity.ok(shiftsService.getAllShiftsUser(pageNo, pageSize, userId, startDate, endDate,
                    shiftStatus, searchKey));
        } catch (Exception e) {
            log.error("Exception while getting all user shifts", e);
            throw e;
        }
    }

    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.shift-management.getById}")
    @GetMapping("${api.endpoints.shift-management.getById}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = ShiftDetailsDto.class))})
    public ResponseEntity<ShiftDetailsDto> getShiftById(@RequestParam String id) {
        try {
            return ResponseEntity.ok(shiftsService.getShiftById(id));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}