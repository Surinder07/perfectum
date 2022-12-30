package ca.waaw.web.rest;

import ca.waaw.web.rest.service.DropdownService;
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
import java.util.Map;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.dropdown}")
public class DropDownController {

    private final DropdownService dropdownService;

    @Operation(description = "${api.description.dropdown.getTimezones}")
    @GetMapping("${api.endpoints.dropdown.getTimezones}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = String.class)))})
    public ResponseEntity<List<String>> getAllTimezones() {
        return ResponseEntity.ok(dropdownService.getAllTimezones());
    }

    @Operation(description = "${api.description.dropdown.getEnums}")
    @GetMapping("${api.endpoints.dropdown.getEnums}")
    public ResponseEntity<Map<String, List<String>>> getAllEnums() {
        return ResponseEntity.ok(dropdownService.getAllEnums());
    }

    @Operation(description = "${api.description.dropdown.getLocations}")
    @GetMapping("${api.endpoints.dropdown.getLocations}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = String.class)))})
    public ResponseEntity<List<Map<String, String>>> getAllLocations() {
        return ResponseEntity.ok(dropdownService.getAllLocations());
    }

    @Operation(description = "${api.description.dropdown.getRoles}")
    @GetMapping("${api.endpoints.dropdown.getRoles}")
    public ResponseEntity<List<Map<String, String>>> getAllLocationRoles(@RequestParam String locationId) {
        return ResponseEntity.ok(dropdownService.getAllLocationRoles(locationId));
    }

    @Operation(description = "${api.description.dropdown.getUsers}")
    @GetMapping("${api.endpoints.dropdown.getUsers}")
    public ResponseEntity<List<Map<String, String>>> getAllUsers() {
        return ResponseEntity.ok(dropdownService.getAllUsers());
    }

}