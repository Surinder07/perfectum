package ca.waaw.web.rest;

import ca.waaw.dto.calender.EventsDto;
import ca.waaw.dto.calender.TimesheetDto;
import ca.waaw.web.rest.service.CalenderService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.calender}")
public class CalenderController {

    private final CalenderService calenderService;

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.calender.getTimesheet}")
    @GetMapping("${api.endpoints.calender.getTimesheet}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = TimesheetDto.class)))})
    public ResponseEntity<List<TimesheetDto>> getTimesheet(@RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(calenderService.getTimesheet(month, year));
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.calender.getDayEvents}")
    @GetMapping("${api.endpoints.calender.getDayEvents}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = EventsDto.class)))})
    public ResponseEntity<List<EventsDto>> getDayEvents(@RequestParam String date) {
        return ResponseEntity.ok(calenderService.getDayEvents(date));
    }

}