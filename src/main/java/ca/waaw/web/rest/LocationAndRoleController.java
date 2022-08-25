package ca.waaw.web.rest;

import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.LocationRoleWithUsersDto;
import ca.waaw.dto.locationandroledtos.NewLocationDto;
import ca.waaw.web.rest.service.LocationAndRoleService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "${api.swagger.groups.location-and-role}")
public class LocationAndRoleController {

    private final LocationAndRoleService locationAndRoleService;

    @SwaggerAuthenticated
    @Operation(description = "${api.description.location-and-role.getLocation}")
    @GetMapping("${api.endpoints.location-and-role.getLocation}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = AdminLocationDto.class))}, description = "${api.swagger.schema-description.getLocation}")
    public ResponseEntity<Object> getLocation() {
        return ResponseEntity.ok(locationAndRoleService.getLocation());
    }

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.location-and-role.addLocation}")
    @PostMapping("${api.endpoints.location-and-role.addLocation}")
    public void addNewLocation(@Valid @RequestBody NewLocationDto newLocationDto) {
        locationAndRoleService.addNewLocation(newLocationDto);
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

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.location-and-role.addLocationRole}")
    @PostMapping("${api.endpoints.location-and-role.addLocationRole}")
    public void addNewLocationRole(@Valid @RequestBody LocationRoleDto locationRoleDto) {
        locationAndRoleService.addNewLocationRole(locationRoleDto);
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

    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @Operation(description = "${api.description.location-and-role.getLocationRole}")
    @GetMapping("${api.endpoints.location-and-role.getLocationRole}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema =
    @Schema(implementation = LocationRoleWithUsersDto.class))}, description = "${api.swagger.schema-description.getLocationRole}")
    public ResponseEntity<Object> getLocationRole(@RequestParam(required = false) @Parameter(
            description = "${api.swagger.param-description.getLocationRole}") String locationRoleId) {
        return ResponseEntity.ok(locationAndRoleService.getLocationRoleInfo(locationRoleId));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.location-and-role.updateLocationRole}")
    @PutMapping("${api.endpoints.location-and-role.updateLocationRole}")
    public void updateLocationRole(@Valid @RequestBody LocationRoleDto locationRoleDto) {
        locationAndRoleService.updateLocationRolePreferences(locationRoleDto);
    }

}