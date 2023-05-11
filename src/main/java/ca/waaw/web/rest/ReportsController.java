package ca.waaw.web.rest;

import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.reports.GenerateReportDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.reports.ReportListingDto;
import ca.waaw.enumration.report.UserReport;
import ca.waaw.web.rest.service.ReportsService;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerOk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.reports}")
public class ReportsController {

    private final ReportsService reportsService;

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.reports.generate}")
    @PostMapping("${api.endpoints.reports.generate}")
    public ResponseEntity<ApiResponseMessageDto> generateReport(@Valid @RequestBody GenerateReportDto reportDto) {
        try {
            return ResponseEntity.ok(reportsService.generateReport(reportDto));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.reports.download}")
    @GetMapping("${api.endpoints.reports.download}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/octet-stream")})
    public ResponseEntity<Resource> downloadReport(@RequestParam String reportId) {
        return reportsService.downloadReport(reportId);
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.reports.get}")
    @GetMapping("${api.endpoints.reports.get}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = ReportListingDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllReports(@PathVariable int pageNo, @PathVariable int pageSize,
                                                       @RequestParam String reportType,
                                                       @RequestParam(required = false) String locationId,
                                                       @RequestParam(required = false) String start,
                                                       @RequestParam(required = false) String end) {
        if (StringUtils.isEmpty(start) || StringUtils.isEmpty(end)) {
            start = null;
            end = null;
        }
        if (StringUtils.isEmpty(locationId)) locationId = null;
        CommonUtils.validateStringInEnum(UserReport.class, reportType, "reportType");
        try {
            return ResponseEntity.ok(reportsService.getAllReports(pageNo, pageSize, reportType, start, end, locationId));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}