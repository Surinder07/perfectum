package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.timesheet.TimesheetDto;
import ca.waaw.dto.timesheet.ActiveTimesheetDto;
import ca.waaw.dto.timesheet.TimesheetDetailsDto;
import ca.waaw.enumration.TimeSheetType;
import ca.waaw.web.rest.service.TimesheetService;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
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

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.timesheet}")
public class TimesheetController {

    private final TimesheetService timesheetService;

    @SwaggerCreated
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.timesheet.startTimer}")
    @PostMapping("${api.endpoints.timesheet.startTimer}")
    public void startTimesheetRecording() {
        timesheetService.startTimesheetRecording();
    }

    @SwaggerOk
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.timesheet.stopTimer}")
    @PutMapping("${api.endpoints.timesheet.stopTimer}")
    public void stopTimesheetRecording() {
        timesheetService.stopTimesheetRecording();
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.timesheet.getTimer}")
    @GetMapping("${api.endpoints.timesheet.getTimer}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = ActiveTimesheetDto.class))})
    public ResponseEntity<ActiveTimesheetDto> getActiveTimesheet() {
        try {
            return ResponseEntity.ok(timesheetService.getActiveTimesheet());
        } catch (Exception e) {
            e.printStackTrace();
            throw  e;
        }
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.timesheet.getAll}")
    @GetMapping("${api.endpoints.timesheet.getAll}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = TimesheetDetailsDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllTimeSheets(@PathVariable int pageNo, @PathVariable int pageSize,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String userId) {
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            startDate = null;
            endDate = null;
        }
        if (StringUtils.isEmpty(type)) type = null;
        else {
            CommonUtils.validateStringInEnum(TimeSheetType.class, type, "type");
        }
        try {
            return ResponseEntity.ok(timesheetService.getAllTimeSheet(pageNo, pageSize, startDate, endDate, type, userId));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.timesheet.getById}")
    @GetMapping("${api.endpoints.timesheet.getById}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = TimesheetDetailsDto.class))})
    public ResponseEntity<TimesheetDetailsDto> getTimeSheetsById(@RequestParam String id) {
        try {
            return ResponseEntity.ok(timesheetService.getTimeSheetsById(id));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.timesheet.add}")
    @PostMapping("${api.endpoints.timesheet.add}")
    public void addNewTimesheet(@Valid @RequestBody TimesheetDto timesheetDto) {
        timesheetService.addNewTimesheet(timesheetDto);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.timesheet.edit}")
    @PutMapping("${api.endpoints.timesheet.edit}")
    public void editTimesheet(@Valid @RequestBody TimesheetDto timesheetDto) {
        timesheetService.editTimesheet(timesheetDto);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.timesheet.delete}")
    @DeleteMapping("${api.endpoints.timesheet.delete}")
    public void deleteTimesheet(@RequestParam String id) {
        timesheetService.deleteTimesheet(id);
    }

}