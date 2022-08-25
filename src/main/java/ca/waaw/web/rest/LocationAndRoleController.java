package ca.waaw.web.rest;

import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.LocationRoleWithUsersDto;
import ca.waaw.dto.locationandroledtos.NewLocationDto;
import ca.waaw.web.rest.service.LocationAndRoleService;
import ca.waaw.web.rest.utils.APIConstants;
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
@Tag(name = APIConstants.TagNames.locationAndRole, description = APIConstants.TagDescription.locationAndRole)
public class LocationAndRoleController {

    private final LocationAndRoleService locationAndRoleService;

    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.getLocation)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = AdminLocationDto.class))},
            description = APIConstants.SchemaDescription.getLocation)
    @GetMapping(APIConstants.ApiEndpoints.LocationAndRole.getLocation)
    public ResponseEntity<Object> getLocation() {
        return ResponseEntity.ok(locationAndRoleService.getLocation());
    }

    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.addLocation)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerBadRequest
    @SwaggerCreated
    @PostMapping(APIConstants.ApiEndpoints.LocationAndRole.addLocation)
    @ResponseStatus(HttpStatus.CREATED)
    public void addNewLocation(@Valid @RequestBody NewLocationDto newLocationDto) {
        locationAndRoleService.addNewLocation(newLocationDto);
    }

    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.deleteLocation)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerBadRequest
    @SwaggerOk
    @DeleteMapping(APIConstants.ApiEndpoints.LocationAndRole.deleteLocation)
    @ResponseStatus(HttpStatus.OK)
    public void deleteLocation(@Valid @RequestParam String id) {
        locationAndRoleService.deleteLocation(id);
    }

    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.addLocationRole)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerBadRequest
    @SwaggerCreated
    @PostMapping(APIConstants.ApiEndpoints.LocationAndRole.addLocationRole)
    @ResponseStatus(HttpStatus.CREATED)
    public void addNewLocationRole(@Valid @RequestBody LocationRoleDto locationRoleDto) {
        locationAndRoleService.addNewLocationRole(locationRoleDto);
    }

    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.deleteLocationRole)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerBadRequest
    @SwaggerOk
    @DeleteMapping(APIConstants.ApiEndpoints.LocationAndRole.deleteLocationRole)
    @ResponseStatus(HttpStatus.OK)
    public void deleteLocationRole(@Valid @RequestParam String id) {
        locationAndRoleService.deleteLocationRole(id);
    }


    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.getLocationRole)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerBadRequest
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LocationRoleWithUsersDto.class))},
            description = APIConstants.SchemaDescription.getLocationRole)
    @GetMapping(APIConstants.ApiEndpoints.LocationAndRole.getLocationRole)
    public ResponseEntity<Object> getLocationRole(@RequestParam(required = false) @Parameter(description = "Required for admins only") String locationRoleId) {
        return ResponseEntity.ok(locationAndRoleService.getLocationRoleInfo(locationRoleId));
    }

    @Operation(summary = APIConstants.ApiDescription.LocationAndRole.updateLocationRole)
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerBadRequest
    @SwaggerOk
    @PutMapping(APIConstants.ApiEndpoints.LocationAndRole.updateLocationRole)
    @ResponseStatus(HttpStatus.OK)
    public void updateLocationRole(@Valid @RequestBody LocationRoleDto locationRoleDto) {
        locationAndRoleService.updateLocationRolePreferences(locationRoleDto);
    }

}