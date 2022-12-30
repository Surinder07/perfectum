package ca.waaw.web.rest.service;

import ca.waaw.dto.EmailReportDto;
import ca.waaw.service.ReportsInternalService;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;

@Service
@AllArgsConstructor
public class ReportsService {

    private final ReportsInternalService reportsInternalService;

    public ResponseEntity<Resource> downloadReport(EmailReportDto reportDto) {
        String fileName = reportsInternalService.getFileName(reportDto);
        ByteArrayResource response = reportsInternalService.getReport(reportDto, fileName);
        return CommonUtils.byteArrayResourceToResponse(response, new StringJoiner(".").add(fileName)
                .add(reportDto.getPreferredFormat()).toString());
    }

    public void emailReport(EmailReportDto reportDto) {
        String fileName = reportsInternalService.getFileName(reportDto);
        reportsInternalService.emailReport(reportDto, fileName);
    }

}