package ca.waaw.service;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.domain.joined.UserReports;
import ca.waaw.dto.EmailReportDto;
import ca.waaw.enumration.UserReport;
import ca.waaw.filehandler.utils.PojoToFileUtils;
import ca.waaw.mapper.ReportsMapper;
import ca.waaw.repository.UserReportsRepository;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class ReportsInternalService {

    private final Logger log = LogManager.getLogger(ReportsInternalService.class);

    private final UserReportsRepository reportsRepository;

    private final AppRegexConfig appRegexConfig;

    public ByteArrayResource getReport(EmailReportDto reportDto, String fileName) {
        return PojoToFileUtils.convertObjectToListOfWritableObject(getReportData(reportDto), fileName,
                reportDto.getPreferredFormat());
    }

    public void emailReport(EmailReportDto reportDto, String fileName) {
        if (reportDto.getEmail() == null || !Pattern.matches(appRegexConfig.getEmail(), reportDto.getEmail())) {
            throw new BadRequestException("Please provide a valid email id", "email");
        }
    }

    private List<Object[]> getReportData(EmailReportDto reportDto) {
        List<UserReports> reportData = new ArrayList<>(); // TODO fetch from repository;
        if (reportDto.getReportType().equalsIgnoreCase(UserReport.ATTENDANCE.toString()))
            return ReportsMapper.getAttendanceReport(reportData);
        else if (reportDto.getReportType().equalsIgnoreCase(UserReport.PAYROLL.toString()))
            return ReportsMapper.getPayrollReport(reportData);
        else throw new BadRequestException("Invalid Report Type");
    }

    public String getFileName(EmailReportDto reportDto) {
        StringJoiner joiner = new StringJoiner("_");
        joiner.add(reportDto.getReportType()).add("report").add(reportDto.getStartDate()).add("-").add(reportDto.getEndDate());
        return joiner.toString();
    }

}