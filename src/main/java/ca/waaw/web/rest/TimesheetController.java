package ca.waaw.web.rest;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.TimesheetDetailDto;
import ca.waaw.dto.TimesheetDto;
import ca.waaw.dto.timeoff.TimeOffInfoDto;
import ca.waaw.web.rest.service.TimesheetService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = DateTimeDto.class))})
    public ResponseEntity<DateTimeDto> getAllTimeOff() {
        return ResponseEntity.ok(timesheetService.getActiveTimesheet());
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.timesheet.getAll}")
    @GetMapping("${api.endpoints.timesheet.getAll}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = TimeOffInfoDto.class)))})
    public ResponseEntity<List<TimesheetDetailDto>> getAllTimeOff(@RequestParam String startDate, @RequestParam String endDate) {
        return ResponseEntity.ok(timesheetService.getAllTimeSheet(startDate, endDate));
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
    @PostMapping("${api.endpoints.timesheet.delete}")
    public void deleteTimesheet(@RequestParam String id) {
        timesheetService.deleteTimesheet(id);
    }

}