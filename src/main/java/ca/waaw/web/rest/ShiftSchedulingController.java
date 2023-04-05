package ca.waaw.web.rest;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.shifts.UpdateShiftDto;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.service.ShiftSchedulingService;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.shift-management}")
public class ShiftSchedulingController {

    private final Logger log = LogManager.getLogger(ShiftSchedulingController.class);

    private final ShiftSchedulingService shiftSchedulingService;

    private final AppRegexConfig appRegexConfig;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.shift-management.createShift}")
    @PostMapping("${api.endpoints.shift-management.createShift}")
    public ResponseEntity<ApiResponseMessageDto> createShift(@Valid @RequestBody NewShiftDto newShiftDto) {
        try {
            return ResponseEntity.ok(shiftSchedulingService.createShift(newShiftDto));
        } catch (Exception e) {
            log.error("Exception while creating shifts", e);
            throw e;
        }
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
            shiftSchedulingService.updateShift(updateShiftDto);
        } catch (Exception e) {
            e.printStackTrace();
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
        shiftSchedulingService.deleteShift(shiftId);
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
        shiftSchedulingService.deleteBatch(batchId);
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

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.shift-management.releaseBatch}")
    @PutMapping("${api.endpoints.shift-management.releaseBatch}")
    public void releaseBatch(@RequestParam String batchId) {
        shiftSchedulingService.releaseBatch(batchId);
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
                                                      @RequestParam(required = false) String shiftStatus) {
        try {
            List<String> field = new ArrayList<>();
            if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    sdf.parse(startDate);
                    sdf.parse(endDate);
                } catch (ParseException e) {
                    throw new BadRequestException("Please enter valid date values (yyyy-MM-dd)", "startDate", "endDate");
                }
            }
            return ResponseEntity.ok(shiftSchedulingService.getAllShifts(pageNo, pageSize, searchKey, locationId, roleId,
                    startDate, endDate, batchStatus, shiftStatus));
        } catch (Exception e) {
            e.printStackTrace();
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
        if (StringUtils.isEmpty(userId)) userId = null;
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            startDate = null;
            endDate = null;
        }
        if (StringUtils.isEmpty(shiftStatus)) shiftStatus = null;
        else CommonUtils.validateStringInEnum(ShiftStatus.class, shiftStatus.toUpperCase(), "shiftStatus");
        if (StringUtils.isEmpty(searchKey)) searchKey = null;
        try {
            return ResponseEntity.ok(shiftSchedulingService.getAllShiftsUser(pageNo, pageSize, userId, startDate, endDate,
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
            return ResponseEntity.ok(shiftSchedulingService.getShiftById(id));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}