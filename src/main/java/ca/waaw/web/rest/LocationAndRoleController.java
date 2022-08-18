package ca.waaw.web.rest;

import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.dto.locationandroledtos.NewLocationDto;
import ca.waaw.web.rest.service.LocationAndRoleService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerCreated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerOk;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerUnauthorized;
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
@Tag(name = "Location and Location_Roles", description = "All Location related apis")
public class LocationAndRoleController {

    private final LocationAndRoleService locationAndRoleService;

    @Operation(summary = "Api to get information location and roles under them.")
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = AdminLocationDto.class))},
            description = "For Global Admin, a list will be returned. For Location Manager a location with list of roles " +
                    "will be returned. For employee single location and single role will be returned.")
    @GetMapping("/v1/location/get")
    public ResponseEntity<Object> getLocation() {
        return ResponseEntity.ok(locationAndRoleService.getLocation());
    }

    @Operation(summary = "Adds a new location under logged in admins organization")
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerCreated
    @PostMapping("/v1/location/save")
    @ResponseStatus(HttpStatus.CREATED)
    public void addNewLocation(@Valid @RequestBody NewLocationDto newLocationDto) {
        locationAndRoleService.addNewLocation(newLocationDto);
    }

    @Operation(summary = "Deletes the location with given Id and suspends the account of related users")
    @SwaggerAuthenticated
    @SwaggerUnauthorized
    @SwaggerOk
    @DeleteMapping("/v1/location/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteLocation(@Valid @RequestParam String id) {
        locationAndRoleService.deleteLocation(id);
    }

}