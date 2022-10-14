package ca.waaw.web.rest;

import ca.waaw.dto.NewTimeOffDto;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.service.TimeOffsService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.timeoff}")
public class TimeoffController {

    private final TimeOffsService timeOffsService;

    // See all timeoff requests with extra option to see allowed ones for admin TODO

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
    @PostMapping("${api.endpoints.timeoff.respond}")
    public void respondToTimeoff(@RequestParam String requestId, @RequestParam boolean accept) {
        timeOffsService.respondToRequest(requestId, accept);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.timeoff.delete}")
    @PutMapping("${api.endpoints.timeoff.delete}")
    public void deleteTimeoff(@RequestParam String requestId) {
        timeOffsService.deleteTimeoff(requestId);
    }

    // Edit timeoff request TODO

}