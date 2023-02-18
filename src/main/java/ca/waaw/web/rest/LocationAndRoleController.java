package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.locationandroledtos.LocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.UpdateLocationRoleDto;
import ca.waaw.enumration.Timezones;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.service.LocationAndRoleService;
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
import java.util.Arrays;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.location-and-role}")
public class LocationAndRoleController {

    private final LocationAndRoleService locationAndRoleService;

    @SwaggerAuthenticated
    @Operation(description = "${api.description.location-and-role.getLocation}")
    @GetMapping("${api.endpoints.location-and-role.getLocation}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = LocationDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getLocation(@PathVariable int pageNo, @PathVariable int pageSize,
                                                     @RequestParam(required = false) String searchKey,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(required = false) String timezone) {
        if (StringUtils.isEmpty(searchKey)) searchKey = null;
        if (StringUtils.isEmpty(timezone)) timezone = null;
        else {
            String finalTimezone = timezone;
            if (Arrays.stream(Timezones.values()).noneMatch(t -> t.value.equals(finalTimezone))) {
                throw new BadRequestException("Invalid timezone", "timezone");
            }
        }
        return ResponseEntity.ok(locationAndRoleService.getLocation(pageNo, pageSize, searchKey, active, timezone));
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.location-and-role.addLocation}")
    @PostMapping("${api.endpoints.location-and-role.addLocation}")
    public void addNewLocation(@Valid @RequestBody LocationDto locationDto) {
        locationAndRoleService.addNewLocation(locationDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.location-and-role.deleteLocation}")
    @DeleteMapping("${api.endpoints.location-and-role.deleteLocation}")
    public void deleteLocation(@Valid @RequestParam String id) {
        locationAndRoleService.deleteLocation(id);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.location-and-role.toggleActiveLocation}")
    @PutMapping("${api.endpoints.location-and-role.toggleActiveLocation}")
    public void toggleActiveLocation(@Valid @RequestParam String id) {
        locationAndRoleService.toggleActiveLocation(id);
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.location-and-role.addLocationRole}")
    @PostMapping("${api.endpoints.location-and-role.addLocationRole}")
    public void addNewLocationRole(@Valid @RequestBody LocationRoleDto locationRoleDto) {
        try {
            locationAndRoleService.addNewLocationRole(locationRoleDto);
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
    @Operation(description = "${api.description.location-and-role.deleteLocationRole}")
    @DeleteMapping("${api.endpoints.location-and-role.deleteLocationRole}")
    public void deleteLocationRole(@Valid @RequestParam String id) {
        locationAndRoleService.deleteLocationRole(id);
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.location-and-role.getLocationRole}")
    @GetMapping("${api.endpoints.location-and-role.getLocationRole}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = PaginationDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getLocationRole(@PathVariable int pageNo, @PathVariable int pageSize,
                                                         @RequestParam(required = false) String searchKey,
                                                         @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) String locationId,
                                                         @RequestParam(required = false) Boolean admin,
                                                         @RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate) {
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            startDate = null;
            endDate = null;
        }
        if (StringUtils.isEmpty(searchKey)) searchKey = null;
        if (StringUtils.isEmpty(locationId)) locationId = null;
        try {
            return ResponseEntity.ok(locationAndRoleService.getLocationRoles(pageNo, pageSize, searchKey, active, locationId,
                    admin, startDate, endDate));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.location-and-role.getLocationRoleById}")
    @GetMapping("${api.endpoints.location-and-role.getLocationRoleById}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = LocationRoleDto.class))})
    public ResponseEntity<LocationRoleDto> getLocationRoleById(@RequestParam String id) {
        return ResponseEntity.ok(locationAndRoleService.getLocationRoleById(id));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.location-and-role.updateLocationRole}")
    @PutMapping("${api.endpoints.location-and-role.updateLocationRole}")
    public void updateLocationRole(@Valid @RequestBody UpdateLocationRoleDto locationRoleDto) {
        locationAndRoleService.updateLocationRolePreferences(locationRoleDto);
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.location-and-role.toggleActiveLocationRole}")
    @PutMapping("${api.endpoints.location-and-role.toggleActiveLocationRole}")
    public void toggleActiveLocationRole(@Valid @RequestParam String id) {
        locationAndRoleService.toggleActiveLocationRole(id);
    }

}