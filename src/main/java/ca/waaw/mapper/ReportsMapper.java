package ca.waaw.mapper;

import ca.waaw.domain.*;
import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.timesheet.Timesheet;
import ca.waaw.domain.user.EmployeePreferencesWithUser;
import ca.waaw.dto.reports.GenerateReportDto;
import ca.waaw.dto.reports.ReportListingDto;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportsMapper {

    /**
     * Maps report entity to dto for listing
     * @param source {@link Reports} entity containing data
     * @param timezone timezone for the logged-in user
     * @return Data mapped to a {@link ReportListingDto} object
     */
    public static ReportListingDto entityToDto(Reports source, String timezone) {
        ReportListingDto target = new ReportListingDto();
        target.setId(source.getId());
        target.setWaawId(source.getWaawId());
        target.setFrom(DateAndTimeUtils.getFullMonthDate(DateAndTimeUtils.getDateInstant(source.getFromDate(), "12:00", timezone), timezone));
        target.setTo(DateAndTimeUtils.getFullMonthDate(DateAndTimeUtils.getDateInstant(source.getToDate(), "12:00", timezone), timezone));
        target.setCreatedOn(DateAndTimeUtils.getFullMonthDate(source.getCreatedDate(), timezone));
        target.setLocationName(source.getLocation().getName());
        return target;
    }

    /**
     * @return list of an object array containing attendance info to be written on a file.
     */
    public static List<Object[]> getAttendanceReport(List<EmployeePreferencesWithUser> employees, List<Timesheet> timesheet,
                                                     List<Shifts> shifts, List<LocationRole> roles, Location location) {
        List<Object[]> response = new ArrayList<>();
        String[] headers = new String[]{"Employee/Contractor Name", "Email Id", "Employee Id", "WAAW Id", "Full Time Employee",
                "Location", "Role", "Shift Start", "Shift End", "Working Start Time", "Working End Time", "Working Timezone",
                "Working Start Time in UTC", "Working End Time in UTC"};
        response.add(headers);
        employees.forEach(employee -> {
            List<Timesheet> employeeSheet = timesheet.stream().filter(sheet -> sheet.getUserId().equals(employee.getUserId()))
                    .collect(Collectors.toList());
            String roleName = roles.stream().filter(role -> role.getId().equals(employee.getLocationRoleId())).findFirst()
                    .orElseThrow().getName();
            shifts.stream().sorted(Comparator.comparing(Shifts::getStart))
                    .filter(shift -> shift.getUserId() != null)
                    .filter(shift -> shift.getUserId().equals(employee.getUserId()))
                    .forEach(shift -> {
                        Object[] row = new Object[headers.length];
                        row[0] = employee.getFullName();
                        row[1] = employee.getEmail();
                        row[2] = employee.getEmployeeId();
                        row[3] = employee.getWaawId();
                        row[4] = employee.isFullTime();
                        row[5] = location.getName();
                        row[6] = roleName;
                        row[7] = DateAndTimeUtils.getDateTimeObject(shift.getStart(), location.getTimezone()).toString();
                        row[8] = DateAndTimeUtils.getDateTimeObject(shift.getEnd(), location.getTimezone()).toString();
                        List<Timesheet> sameDay = employeeSheet.stream()
                                .filter(sheet -> DateAndTimeUtils.isInstantSameDayAsAnotherInstant(sheet.getStart(), shift.getStart()))
                                .collect(Collectors.toList());
                        if (sameDay.size() > 0) {
                            sameDay.forEach(employeeSheet::remove);
                            row[9] = DateAndTimeUtils.getDateTimeObject(sameDay.get(0).getStart(), location.getTimezone()).toString();
                            row[10] = DateAndTimeUtils.getDateTimeObject(sameDay.get(0).getEnd(), location.getTimezone()).toString();
                            row[11] = location.getTimezone();
                            row[12] = DateAndTimeUtils.getDateTimeObject(sameDay.get(0).getStart(), "UTC").toString();
                            row[13] = DateAndTimeUtils.getDateTimeObject(sameDay.get(0).getEnd(), "UTC").toString();
                        }
                        response.add(row);
                        if (sameDay.size() > 1) {
                            sameDay.forEach(sheet -> {
                                if (sameDay.indexOf(sheet) == 0) return;
                                Object[] extraRow = new Object[headers.length];
                                extraRow[9] = DateAndTimeUtils.getDateTimeObject(sheet.getStart(), location.getTimezone()).toString();
                                extraRow[10] = DateAndTimeUtils.getDateTimeObject(sheet.getEnd(), location.getTimezone()).toString();
                                extraRow[11] = location.getTimezone();
                                extraRow[12] = DateAndTimeUtils.getDateTimeObject(sheet.getStart(), "UTC").toString();
                                extraRow[13] = DateAndTimeUtils.getDateTimeObject(sheet.getEnd(), "UTC").toString();
                                response.add(extraRow);
                            });
                        }
                    });
            if (employeeSheet.size() > 0) {
                employeeSheet.forEach(sheet -> {
                    Object[] row = new Object[headers.length];
                    row[0] = employee.getFullName();
                    row[1] = employee.getEmail();
                    row[2] = employee.getEmployeeId();
                    row[3] = employee.getWaawId();
                    row[4] = employee.isFullTime();
                    row[5] = location.getName();
                    row[6] = roleName;
                    row[7] = "N/A";
                    row[8] = "N/A";
                    row[9] = DateAndTimeUtils.getDateTimeObject(sheet.getStart(), location.getTimezone()).toString();
                    row[10] = DateAndTimeUtils.getDateTimeObject(sheet.getEnd(), location.getTimezone()).toString();
                    row[11] = location.getTimezone();
                    row[12] = DateAndTimeUtils.getDateTimeObject(sheet.getStart(), "UTC").toString();
                    row[13] = DateAndTimeUtils.getDateTimeObject(sheet.getEnd(), "UTC").toString();
                    response.add(row);
                });
            }
        });
        return response;
    }

    /**
     * @return list of an object array containing payroll info to be written on a file.
     */
    public static List<Object[]> getPayrollReport(List<EmployeePreferencesWithUser> employees, List<Timesheet> timesheet,
                                                  List<Requests> requests, List<Shifts> shifts, List<LocationRole> roles,
                                                  Location location, Set<OrganizationHolidays> holidays,
                                                  GenerateReportDto reportDto) {
        List<Object[]> response = new ArrayList<>();
        String[] headers = new String[]{"Employee/Contractor Name", "Email Id", "Employee Id", "WAAW Id", "Full Time Employee",
                "Location", "Role", "Shifts Assigned in Hours(hh:mm)", "Vacation in Hours(hh:mm)",
                "Sick Leaves in Hours(hh:mm)", "Hours Worked(hh:mm)", "Statutory Holiday(hh:mm)", "Hourly Rate"};
        response.add(headers);
        List<OrganizationHolidays> holidayList = getRelevantHolidays(holidays, reportDto.getStartDate(), reportDto.getEndDate());
        employees.forEach(employee -> {
            Object[] row = new Object[headers.length];
            row[0] = employee.getFullName();
            row[1] = employee.getEmail();
            row[2] = employee.getEmployeeId();
            row[3] = employee.getWaawId();
            row[4] = employee.isFullTime();
            row[5] = location.getName();
            row[6] = roles.stream().filter(role -> role.getId().equals(employee.getLocationRoleId())).findFirst()
                    .orElseThrow().getName();
            long shiftsAssigned = shifts.stream()
                    .filter(shift -> StringUtils.isNotEmpty(shift.getUserId()))
                    .filter(shift -> shift.getUserId().equals(employee.getUserId()))
                    .mapToLong(shift -> Duration.between(shift.getStart(), shift.getEnd()).toMinutes())
                    .sum();
            long hoursWorked = timesheet.stream()
                    .filter(shift -> StringUtils.isNotEmpty(shift.getUserId()))
                    .filter(sheet -> sheet.getUserId().equals(employee.getUserId()))
                    .mapToLong(sheet -> Duration.between(sheet.getStart(), sheet.getEnd()).toMinutes())
                    .sum();
            long vacation = requests.stream()
                    .filter(request -> request.getUserId().equals(employee.getUserId()))
                    .filter(request -> request.getSubType().equals(RequestSubType.VACATION_LEAVE_FULL_DAY) ||
                            request.getSubType().equals(RequestSubType.VACATION_LEAVE_HALF_DAY))
                    .mapToLong(request -> Duration.between(request.getStart(), request.getEnd()).toMinutes())
                    .sum();
            long sickLeave = requests.stream()
                    .filter(request -> request.getUserId().equals(employee.getUserId()))
                    .filter(request -> request.getSubType().equals(RequestSubType.SICK_LEAVE_HALF_DAY) ||
                            request.getSubType().equals(RequestSubType.SICK_LEAVE_FULL_DAY))
                    .mapToLong(request -> Duration.between(request.getStart(), request.getEnd()).toMinutes())
                    .sum();
            long holidayTime = holidayList.stream()
                    .mapToLong(holiday -> validateHolidayAndGetTime(holiday, employee))
                    .sum();
            row[7] = DateAndTimeUtils.getHourMinuteTimeFromMinutes(shiftsAssigned).toString();
            row[8] = DateAndTimeUtils.getHourMinuteTimeFromMinutes(vacation).toString();
            row[9] = DateAndTimeUtils.getHourMinuteTimeFromMinutes(sickLeave).toString();
            row[10] = DateAndTimeUtils.getHourMinuteTimeFromMinutes(hoursWorked).toString();
            row[11] = DateAndTimeUtils.getHourMinuteTimeFromMinutes(holidayTime).toString();
            row[12] = employee.getWagesCurrency() != null ? employee.getWagesPerHour() + " " + employee.getWagesCurrency().toString() : "-";
            response.add(row);
        });
        return response;
    }

    public static List<Object[]> getHolidayReport(Location location, Set<OrganizationHolidays> holidays,
                                                  GenerateReportDto reportDto) {
        List<OrganizationHolidays> holidayList = getRelevantHolidays(holidays, reportDto.getStartDate(), reportDto.getEndDate());
        List<Object[]> response = new ArrayList<>();
        String[] headers = new String[]{"Location Name", "Holiday Name", "Holiday Type", "Year", "Month",
                "Day"};
        response.add(headers);
        holidayList.forEach(holiday -> {
            Object[] row = new Object[headers.length];
            row[0] = location.getName();
            row[1] = holiday.getName();
            row[2] = holiday.getType().toString();
            row[3] = holiday.getYear();
            row[4] = holiday.getMonth();
            row[5] = holiday.getDate();
            response.add(row);
        });
        return response;
    }

    private static List<OrganizationHolidays> getRelevantHolidays(Set<OrganizationHolidays> holidays, String startDate,
                                                                  String endDate) {
        String[] start = startDate.split("-");
        String[] end = endDate.split("-");
        return holidays.stream()
                .filter(holiday -> {
                    boolean allowed = holiday.getYear() >= Integer.parseInt(start[0]) || holiday.getYear() <= Integer.parseInt(end[0]);
                    if ((holiday.getYear() == Integer.parseInt(start[0]) && holiday.getMonth() < Integer.parseInt(start[1])) ||
                            (holiday.getYear() == Integer.parseInt(end[0]) && holiday.getMonth() > Integer.parseInt(end[1])))
                        allowed = false;
                    if ((holiday.getYear() == Integer.parseInt(start[0]) && holiday.getMonth() == Integer.parseInt(start[1]) &&
                            holiday.getDate() < Integer.parseInt(start[2])) || (holiday.getYear() == Integer.parseInt(end[0]) &&
                            holiday.getMonth() == Integer.parseInt(end[1]) && holiday.getDate() > Integer.parseInt(end[2])))
                        allowed = false;
                    return allowed;
                })
                .collect(Collectors.toList());
    }

    private static long validateHolidayAndGetTime(OrganizationHolidays holiday, EmployeePreferencesWithUser employee) {
        DayOfWeek day = DateAndTimeUtils.toDate(holiday.getYear(), holiday.getMonth(), holiday.getDate(), "UTC")
                .atZone(ZoneId.of("UTC")).getDayOfWeek();
        switch (day) {
            case SUNDAY:
                return employee.getSundayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getSundayStartTime(), employee.getSundayEndTime());
            case MONDAY:
                return employee.getMondayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getMondayStartTime(), employee.getMondayEndTime());
            case TUESDAY:
                return employee.getTuesdayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getTuesdayStartTime(), employee.getTuesdayEndTime());
            case WEDNESDAY:
                return employee.getWednesdayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getWednesdayStartTime(), employee.getWednesdayEndTime());
            case THURSDAY:
                return employee.getThursdayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getThursdayStartTime(), employee.getThursdayEndTime());
            case FRIDAY:
                return employee.getFridayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getFridayStartTime(), employee.getFridayEndTime());
            case SATURDAY:
                return employee.getSaturdayStartTime() == null ? 0 :
                        DateAndTimeUtils.getSameDayTimeDifference(employee.getSaturdayStartTime(), employee.getSaturdayEndTime());
            default:
                return 0L;
        }
    }

}
