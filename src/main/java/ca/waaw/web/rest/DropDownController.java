package ca.waaw.web.rest;

import ca.waaw.enumration.Timezones;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.dropdown}")
public class DropDownController {

    @Operation(description = "${api.description.dropdown.getTimezones}")
    @GetMapping("${api.endpoints.dropdown.getTimezones}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = String.class)))})
    public ResponseEntity<List<String>> getAllTimezones() {
        return ResponseEntity.ok(Arrays.stream(Timezones.values()).map(zone -> zone.value)
                .collect(Collectors.toList()));
    }

}