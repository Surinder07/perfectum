package ca.waaw.web.rest;

import ca.waaw.dto.EmailReportDto;
import ca.waaw.web.rest.service.ReportsService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerOk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.reports}")
public class ReportsController {

    private final ReportsService reportsService;

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.reports.download}")
    @GetMapping("${api.endpoints.reports.download}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/octet-stream")})
    public ResponseEntity<Resource> downloadReport(@Valid @RequestBody EmailReportDto reportDto) {
        return reportsService.downloadReport(reportDto);
    }

    @SwaggerOk
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.reports.email}")
    @GetMapping("${api.endpoints.reports.email}")
    public void emailReport(@Valid @RequestBody EmailReportDto reportDto) {
        reportsService.emailReport(reportDto);
    }

}