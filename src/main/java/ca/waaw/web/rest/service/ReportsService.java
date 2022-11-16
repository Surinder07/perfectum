package ca.waaw.web.rest.service;

import ca.waaw.dto.EmailReportDto;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;

@Service
@AllArgsConstructor
public class ReportsService {

    private final Logger log = LogManager.getLogger(ReportsService.class);

    public ResponseEntity<Resource> downloadReport(EmailReportDto reportDto) {
        return null;
    }

    public void emailReport(EmailReportDto reportDto) {

    }

    private String getFileName(EmailReportDto reportDto) {
        StringJoiner joiner = new StringJoiner("-");
        joiner.add(reportDto.getReportType() + "_report").add(reportDto.getStartDate()).add(reportDto.getEndDate());
        return joiner.toString();
    }

}