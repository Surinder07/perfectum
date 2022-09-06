package ca.waaw.web.rest;

import ca.waaw.dto.HolidayDto;
import ca.waaw.dto.userdtos.OrganizationPreferences;
import ca.waaw.web.rest.service.OrganizationService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.organization}")
public class OrganizationController {

    private final OrganizationService organizationService;

    @SwaggerOk
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.updateOrganizationPreferences}")
    @PutMapping("${api.endpoints.organization.updateOrganizationPreferences}")
    public void updateOrganizationPreferences(@RequestBody OrganizationPreferences preferences) {
        organizationService.updateOrganizationPreferences(preferences);
    }

    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @Operation(description = "${api.description.organization.getHolidays}")
    @GetMapping("${api.endpoints.organization.getHolidays}")
    @ApiResponse(responseCode = "200", description = "${api.swagger.schema-description.getAllHolidays}", content =
            {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = HolidayDto.class)))})
    public ResponseEntity<Object> getAllHolidays() {
        return null;
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.addHolidaysExcel}")
    @PostMapping("${api.endpoints.organization.addHolidaysExcel}")
    public void uploadHolidaysByExcel(@RequestPart MultipartFile file, @RequestPart(required = false) String locationId) {

    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.addHoliday}")
    @PostMapping("${api.endpoints.organization.addHoliday}")
    public void addHoliday(@Valid @RequestBody HolidayDto holidayDto) {

    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.editHoliday}")
    @PutMapping("${api.endpoints.organization.editHoliday}")
    public void editHoliday(@Valid @RequestBody HolidayDto holidayDto) {

    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.deleteHoliday}")
    @DeleteMapping("${api.endpoints.organization.deleteHoliday}")
    public void deleteHoliday(@RequestParam String holidayId) {

    }

}