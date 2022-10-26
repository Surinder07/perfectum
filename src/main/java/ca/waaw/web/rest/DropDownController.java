package ca.waaw.web.rest;

import ca.waaw.enumration.*;
import ca.waaw.web.rest.errors.exceptions.ForDevelopmentOnlyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.dropdown}")
public class DropDownController {

    private Environment environment;

    @Operation(description = "${api.description.dropdown.getTimezones}")
    @GetMapping("${api.endpoints.dropdown.getTimezones}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = String.class)))})
    public ResponseEntity<List<String>> getAllTimezones() {
        return ResponseEntity.ok(Arrays.stream(Timezones.values()).map(zone -> zone.value)
                .collect(Collectors.toList()));
    }

    @Operation(description = "${api.description.dropdown.getEnums}")
    @GetMapping("${api.endpoints.dropdown.getEnums}")
    public ResponseEntity<Map<String, List<String>>> getAllEnums() {
        if (!Boolean.parseBoolean(environment.getProperty("springdoc.swagger-ui.enabled"))) {
            throw new ForDevelopmentOnlyException();
        }
        Map<String, List<String>> enumMap = new HashMap<>();
        populateListToEnumMap(enumMap, Authority.class);
        populateListToEnumMap(enumMap, HolidayType.class);
        populateListToEnumMap(enumMap, NotificationType.class);
        populateListToEnumMap(enumMap, PromoCodeType.class);
        populateListToEnumMap(enumMap, ShiftStatus.class);
        populateListToEnumMap(enumMap, ShiftType.class);
        populateListToEnumMap(enumMap, PayrollGenerationType.class);
        return ResponseEntity.ok(enumMap);
    }

    private static void populateListToEnumMap(Map<String, List<String>> map, Class<? extends Enum<?>> enumClass) {
        List<?> valuesToIgnore = List.of(new Object[]{Authority.SUPER_USER, Authority.ANONYMOUS});
        map.put(enumClass.getSimpleName(), Stream.of(enumClass.getEnumConstants())
                .filter(value -> !valuesToIgnore.contains(value))
                .map(Objects::toString).collect(Collectors.toList()));
    }

}