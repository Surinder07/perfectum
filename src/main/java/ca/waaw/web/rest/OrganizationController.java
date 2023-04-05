package ca.waaw.web.rest;

import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.holiday.HolidayDto;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.organization}")
public class OrganizationController {

    private final OrganizationService organizationService;

    private final Logger log = LogManager.getLogger(OrganizationController.class);

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
    public ResponseEntity<List<HolidayDto>> getAllHolidays(@RequestParam(required = false) Integer year) {
        try {
            return ResponseEntity.ok(organizationService.getAllHolidays(year));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @SwaggerRespondWithMessage
    @Operation(description = "${api.description.organization.addHolidaysExcel}")
    @PostMapping("${api.endpoints.organization.addHolidaysExcel}")
    public ResponseEntity<ApiResponseMessageDto> uploadHolidaysByExcel(@RequestPart MultipartFile file) {
        try {
            return ResponseEntity.ok(organizationService.uploadHolidaysByExcel(file));
        } catch (Exception e) {
            log.error("Exception while uploading holidays", e);
            throw e;
        }
    }

    @SwaggerCreated
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.organization.addHoliday}")
    @PostMapping("${api.endpoints.organization.addHoliday}")
    public void addHoliday(@Valid @RequestBody HolidayDto holidayDto) {
        organizationService.addHoliday(holidayDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.editHoliday}")
    @PutMapping("${api.endpoints.organization.editHoliday}")
    public void editHoliday(@RequestBody HolidayDto holidayDto) {
        organizationService.editHoliday(holidayDto);
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
        organizationService.deleteHoliday(holidayId);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.organization.uploadLogo}")
    @PostMapping("${api.endpoints.organization.uploadLogo}")
    public void updateOrganizationLogo(@RequestPart MultipartFile file) throws Exception {
        organizationService.updateOrganizationLogo(file);
    }

}