package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppMailConfig;
import ca.waaw.domain.Reports;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.reports.GenerateReportDto;
import ca.waaw.dto.appnotifications.MailDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.FileType;
import ca.waaw.enumration.report.UserReport;
import ca.waaw.mapper.ReportsMapper;
import ca.waaw.repository.ReportsRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.ReportsInternalService;
import ca.waaw.service.email.javamailsender.MailService;
import ca.waaw.service.email.javamailsender.TempMailService;
import ca.waaw.storage.AzureStorage;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.MessageConstants;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class ReportsService {

    private final UserOrganizationRepository userOrganizationRepository;

    private final ReportsInternalService reportsInternalService;

    private final ReportsRepository reportsRepository;

    private final AzureStorage azureStorage;

    private final AppMailConfig appMailConfig;

    private final MailService mailService;

    private final TempMailService tempMailService;

    private final AppCustomIdConfig appCustomIdConfig;

    public ApiResponseMessageDto generateReport(GenerateReportDto reportDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String fileName = reportsInternalService.getFileName(reportDto, "xls");
        ByteArrayResource dataForFile = reportsInternalService.getReport(reportDto, fileName, loggedUser);
        String uploadedFile = azureStorage.uploadFile(fileName, dataForFile.getByteArray(), FileType.valueOf(reportDto.getReportType()));
        Reports reports = new Reports();
        reports.setCreatedBy(loggedUser.getId());
        reports.setFromDate(reportDto.getStartDate());
        reports.setToDate(reportDto.getEndDate());
        reports.setType(UserReport.valueOf(reportDto.getReportType()));
        reports.setOrganizationId(loggedUser.getOrganizationId());
        reports.setLocationId(loggedUser.getAuthority().equals(Authority.MANAGER) ? loggedUser.getLocationId() :
                reportDto.getLocationId());
        reports.setFileName(uploadedFile);
        reports.setShowToManger(loggedUser.getAuthority().equals(Authority.MANAGER));
        String currentCustomId = reportsRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getReportPrefix() + "0000000000");
        reports.setWaawId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
        reportsRepository.save(reports);
        MailDto mailDto = MailDto.builder()
                .name(loggedUser.getFullName())
                .email(loggedUser.getEmail())
                .actionUrl(appMailConfig.getUiUrl())
                .langKey(loggedUser.getLangKey())
                .buttonText("Go to WaaW")
//                .message("You generated a report from " + reportDto.getStartDate() + " to " + reportDto.getEndDate() +
//                        ". Please find the report attached below.")
//                .title(reportDto.getReportType().toLowerCase() + " report ")
                .build();
        tempMailService.sendEmailFromTemplate(mailDto, MessageConstants.emailReportGenerate, null,
                new String[] {reportDto.getStartDate(), reportDto.getEndDate()}, dataForFile, fileName);
//        mailService.sendEmailFromTemplate(mailDto, "mail/TitleDescriptionTemplate", dataForFile, fileName, "email.report.generate.title");
        return new ApiResponseMessageDto("You will receive the report on your email once its generated.");
    }

    public ResponseEntity<Resource> downloadReport(String reportId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Reports report = reportsRepository.findOneByIdAndDeleteFlag(reportId, false)
                .orElseThrow(() -> new EntityNotFoundException("report"));
        ByteArrayResource resource = new ByteArrayResource(azureStorage.retrieveFileData(report.getFileName(),
                FileType.valueOf(report.getType().toString())));
        return CommonUtils.byteArrayResourceToResponse(resource, report.getFileName());
    }

    public PaginationDto getAllReports(int pageNo, int pageSize, String reportType, String start, String end, String locationId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                loggedUser.getLocation().getTimezone();
        if (loggedUser.getAuthority().equals(Authority.MANAGER)) locationId = loggedUser.getLocationId();
        Instant[] dateRange = start != null ? DateAndTimeUtils.getStartAndEndTimeForInstant(start, end, timezone) :
                new Instant[]{null, null};
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        Page<Reports> reportsPage = reportsRepository.getAllWithFilters(loggedUser.getOrganizationId(), locationId,
                loggedUser.getAuthority().equals(Authority.MANAGER), dateRange[0], dateRange[1], UserReport.valueOf(reportType),
                getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(reportsPage, ReportsMapper::entityToDto, timezone);
    }

}