package ca.waaw.web.rest;

import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.service.ResourceService;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringJoiner;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.resources}")
public class ResourceController {

    private final ResourceService resourceService;

    @SwaggerBadRequest
    @Operation(description = "${api.description.resources.downloadSample}")
    @GetMapping("${api.endpoints.resources.downloadSample}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/octet-stream")})
    public ResponseEntity<Resource> downloadSampleFile(@RequestParam String resource, @RequestParam(required = false) String format) throws IOException {
        if (!(format.equalsIgnoreCase("csv") || format.equalsIgnoreCase("xlsx"))) {
            throw new BadRequestException("Invalid Format");
        }
        String fileName;
        switch (resource) {
            case "inviteUser":
                fileName = "NewEmployeeTemplate";
                break;
            case "holiday":
                fileName = "HolidaysTemplate";
                break;
            default:
                throw new BadRequestException("Invalid resource");
        }
        InputStream is = ResourceController.class.getResourceAsStream("/templates/fileTemplates/" + fileName + '.' + format);
        assert is != null;
        return CommonUtils.byteArrayResourceToResponse(new ByteArrayResource(is.readAllBytes()),
                new StringJoiner(".").add(fileName).add(format).toString());
    }

    @SwaggerBadRequest
    @Operation(description = "${api.description.resources.getImageLink}")
    @GetMapping("${api.endpoints.resources.getImageLink}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/octet-stream")})
    public ResponseEntity<byte[]> getImageLink(@PathVariable String type, @PathVariable String id) throws IOException {
        return ResponseEntity.ok(resourceService.getImageLink(type, id));
    }

}