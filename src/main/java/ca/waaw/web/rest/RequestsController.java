package ca.waaw.web.rest;

import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.requests.NewRequestDto;
import ca.waaw.dto.requests.UpdateRequestDto;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestType;
import ca.waaw.web.rest.service.RequestsService;
import ca.waaw.web.rest.utils.CommonUtils;
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

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.requests}")
public class RequestsController {

    private final RequestsService requestsService;

    @SwaggerCreated
    @SwaggerBadRequest
    @SwaggerUnauthorized
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "${api.description.requests.newRequest}")
    @PostMapping("${api.endpoints.requests.newRequest}")
    private void addNewRequest(@Valid @RequestBody NewRequestDto newRequestDto) {
        try {
            requestsService.addNewRequest(newRequestDto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.requests.getAll}")
    @GetMapping("${api.endpoints.requests.getAll}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = BatchDetailsDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAll(@PathVariable int pageNo, @PathVariable int pageSize,
                                                @RequestParam(required = false) String searchKey,
                                                @RequestParam(required = false) String locationId,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String type) {
        if (StringUtils.isEmpty(searchKey)) searchKey = null;
        if (StringUtils.isEmpty(locationId)) locationId = null;
        if (StringUtils.isEmpty(status)) status = null;
        else CommonUtils.validateStringInEnum(RequestStatus.class, status, "status");
        if (StringUtils.isEmpty(type)) type = null;
        else CommonUtils.validateStringInEnum(RequestType.class, type, "type");
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            startDate = null;
            endDate = null;
        }
        try {
            return ResponseEntity.ok(requestsService.getAllRequests(pageNo, pageSize, searchKey, locationId, startDate, endDate, status, type));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerAuthenticated
    @Operation(description = "${api.description.requests.getByUser}")
    @GetMapping("${api.endpoints.requests.getByUser}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = BatchDetailsDto.class))}, description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getByUser(@PathVariable int pageNo, @PathVariable int pageSize,
                                                   @RequestParam(required = false) String userId,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String type) {
        if (StringUtils.isEmpty(userId)) userId = null;
        if (StringUtils.isEmpty(status)) status = null;
        else CommonUtils.validateStringInEnum(RequestStatus.class, status, "status");
        if (StringUtils.isEmpty(type)) type = null;
        else CommonUtils.validateStringInEnum(RequestType.class, type, "type");
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            startDate = null;
            endDate = null;
        }
        try {
            return ResponseEntity.ok(requestsService.getAllRequestsForUser(pageNo, pageSize, userId, startDate, endDate, status, type));
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
    @Operation(description = "${api.description.requests.updateRequest}")
    @PutMapping("${api.endpoints.requests.updateRequest}")
    public void updateRequest(@RequestBody UpdateRequestDto dto) {
        try {
            requestsService.updateRequest(dto);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}