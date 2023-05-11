package ca.waaw.service;

import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.timesheet.Timesheet;
import ca.waaw.domain.user.EmployeePreferencesWithUser;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.reports.GenerateReportDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestType;
import ca.waaw.enumration.report.UserReport;
import ca.waaw.filehandler.utils.PojoToFileUtils;
import ca.waaw.mapper.ReportsMapper;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.locationandroles.LocationRoleRepository;
import ca.waaw.repository.organization.OrganizationHolidayRepository;
import ca.waaw.repository.requests.RequestsRepository;
import ca.waaw.repository.timesheet.TimesheetRepository;
import ca.waaw.repository.user.EmployeePreferencesWithUserRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReportsInternalService {

    private final Logger log = LogManager.getLogger(ReportsInternalService.class);

    private final EmployeePreferencesWithUserRepository employeePreferencesWithUserRepository;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final TimesheetRepository timesheetRepository;

    private final RequestsRepository requestsRepository;

    private final ShiftsRepository shiftsRepository;

    private final OrganizationHolidayRepository organizationHolidayRepository;

    public ByteArrayResource getReport(GenerateReportDto reportDto, String fileName, UserOrganization loggedUser) {
        return PojoToFileUtils.convertObjectToListOfWritableObject(getReportData(reportDto, loggedUser), fileName,
                "xls");
    }

    public List<Object[]> getReportData(GenerateReportDto reportDto, UserOrganization loggedUser) {
        String locationId = StringUtils.isNotEmpty(loggedUser.getLocationId()) ? loggedUser.getLocationId() : reportDto.getLocationId();

        List<EmployeePreferencesWithUser> employees = employeePreferencesWithUserRepository.findAllByLocationIdAndIsExpiredAndDeleteFlag(locationId, false, false);
        List<LocationRole> roles = locationRoleRepository.findAllByDeleteFlagAndIdIn(false,
                        employees.stream().map(EmployeePreferencesWithUser::getLocationRoleId).collect(Collectors.toList()))
                .stream().filter(role -> {
                    if (loggedUser.getAuthority().equals(Authority.ADMIN)) return true;
                    else return !role.isAdminRights();
                }).collect(Collectors.toList());
        if (!loggedUser.getAuthority().equals(Authority.ADMIN)) {
            employees = employees.stream().filter(emp -> roles.stream().map(LocationRole::getId)
                            .collect(Collectors.toList()).contains(emp.getLocationRoleId()))
                    .collect(Collectors.toList());
        }
        Location location = locationRepository.findOneByIdAndDeleteFlag(locationId, false)
                .orElseThrow(() -> new EntityNotFoundException("location"));
        Instant[] dateRange = DateAndTimeUtils.getStartAndEndTimeForInstant(reportDto.getStartDate(), reportDto.getEndDate(), location.getTimezone());
        List<Timesheet> timesheet = timesheetRepository.findAllByLocationIdAndStartBetweenAndDeleteFlag(locationId, dateRange[0],
                dateRange[1], false);
        List<Requests> requests = requestsRepository.findAllByLocationIdAndStartBetweenAndDeleteFlag(locationId, dateRange[0],
                        dateRange[1], false).stream()
                .filter(req -> req.getType().equals(RequestType.TIME_OFF))
                .filter(req -> req.getStatus().equals(RequestStatus.ACCEPTED))
                .collect(Collectors.toList());
        List<Shifts> shifts = shiftsRepository.findAllByLocationIdAndStartBetweenAndDeleteFlag(locationId, dateRange[0],
                dateRange[1], false);
        Set<OrganizationHolidays> holidays = new HashSet<>(organizationHolidayRepository.getAllForLocationByYear(locationId,
                Integer.parseInt(reportDto.getStartDate().split("-")[0])));
        holidays.addAll(new HashSet<>(organizationHolidayRepository.getAllForLocationByYear(locationId,
                Integer.parseInt(reportDto.getEndDate().split("-")[0]))));
        if (reportDto.getReportType().equals(UserReport.ATTENDANCE.toString())) {
            return ReportsMapper.getAttendanceReport(employees, timesheet, shifts, roles, location);
        } else if (reportDto.getReportType().equals(UserReport.PAYROLL.toString())) {
            return ReportsMapper.getPayrollReport(employees, timesheet, requests, shifts, roles, location, holidays, reportDto);
        } else if (reportDto.getReportType().equals(UserReport.HOLIDAYS.toString())) {
            return ReportsMapper.getHolidayReport(location, holidays, reportDto);
        }
        return null;
    }

    public String getFileName(GenerateReportDto reportDto, String extension) {
        StringJoiner joiner = new StringJoiner("_");
        joiner.add(reportDto.getReportType()).add("report").add(reportDto.getStartDate())
                .add(reportDto.getEndDate()).add(UUID.randomUUID().toString().split("-")[0]);
        return joiner + "." + extension;
    }

}