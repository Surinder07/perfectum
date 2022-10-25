package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.timeoff.NewTimeOffDto;
import ca.waaw.dto.timeoff.TimeOffInfoDto;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.service.TimeOffsService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.timeoff}")
public class TimeoffController {

    private final TimeOffsService timeOffsService;

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.timeoff.get}")
    @GetMapping("${api.endpoints.timeoff.get}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = TimeOffInfoDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllTimeOff(@PathVariable int pageNo, @PathVariable int pageSize,
                                                       @Parameter(description = "${api.swagger.param-description.timeoff-showAll}")
                                                       @RequestParam(required = false) boolean showAll,
                                                       @Parameter(description = "${api.swagger.param-description.timeoff-date}")
                                                       @RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(timeOffsService.getAllTimeOff(pageNo, pageSize, showAll, startDate, endDate));
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerAlreadyExist
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.timeoff.add}")
    @PostMapping("${api.endpoints.timeoff.add}")
    public void addNewTimeoff(@Valid @RequestBody NewTimeOffDto newTimeOffDto) {
        if (newTimeOffDto.getStartDate().getDate() == null || newTimeOffDto.getEndDate().getDate() == null)
            throw new BadRequestException("Required fields are missing", "startDate", "endDate");
        timeOffsService.addNewTimeoff(newTimeOffDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.timeoff.respond}")
    @PutMapping("${api.endpoints.timeoff.respond}")
    public void respondToTimeoff(@RequestParam String requestId, @RequestParam boolean accept) {
        timeOffsService.respondToRequest(requestId, accept);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.timeoff.delete}")
    @DeleteMapping("${api.endpoints.timeoff.delete}")
    public void deleteTimeoff(@RequestParam String requestId) {
        timeOffsService.deleteTimeoff(requestId);
    }

    // Edit timeoff request TODO

}