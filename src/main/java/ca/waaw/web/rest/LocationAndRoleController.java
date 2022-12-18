package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.locationandroledtos.LocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.UpdateLocationRoleDto;
import ca.waaw.web.rest.service.LocationAndRoleService;
import ca.waaw.web.rest.utils.customannotations.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
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
            implementation = LocationDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getLocation(@PathVariable int pageNo, @PathVariable int pageSize) {
        return ResponseEntity.ok(locationAndRoleService.getLocation(pageNo, pageSize));
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

    @SwaggerAuthenticated
    @Operation(description = "${api.description.location-and-role.getLocationRole}")
    @GetMapping("${api.endpoints.location-and-role.getLocationRole}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(
            implementation = PaginationDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getLocationRole(@PathVariable int pageNo, @PathVariable int pageSize) {
        return ResponseEntity.ok(locationAndRoleService.getLocationRoles(pageNo, pageSize));
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