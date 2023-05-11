package ca.waaw.web.rest.service;

import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.domain.shifts.ShiftDetails;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.user.User;
import ca.waaw.domain.timesheet.DetailedTimesheet;
//import ca.waaw.domain.joined.ShiftDetailsWithBatch;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.*;
//import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.locationandroles.LocationRoleUsersRepository;
import ca.waaw.repository.locationandroles.LocationUsersRepository;
import ca.waaw.repository.organization.OrganizationHolidayRepository;
import ca.waaw.repository.requests.DetailedRequestsRepository;
import ca.waaw.repository.requests.RequestsRepository;
import ca.waaw.repository.shifts.ShiftDetailsRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.timesheet.DetailedTimesheetRepository;
import ca.waaw.repository.timesheet.TimesheetRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DashboardService {

    private final Logger log = LogManager.getLogger(DashboardService.class);

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final LocationRepository locationRepository;

    private final LocationUsersRepository locationUsersRepository;

    private final LocationRoleUsersRepository locationRoleUsersRepository;

    private final ShiftDetailsRepository shiftDetailsRepository;

    private final TimesheetRepository timesheetRepository;

    private final DetailedTimesheetRepository detailedTimesheetRepository;

    private final RequestsRepository requestsRepository;

    private final DetailedRequestsRepository detailedRequestsRepository;

    private final ShiftsRepository shiftsRepository;

    private final OrganizationHolidayRepository organizationHolidayRepository;

    public Map<String, Object> getDashboardData() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    Map<String, Object> response = new HashMap<>();
                    Map<String, Object> tilesInfo = new HashMap<>();
                    tilesInfo.put("holidayCurrentWeek", getCurrentWeekHolidays(loggedUser));
                    tilesInfo.put("pendingRequests", getPendingRequests(loggedUser));
                    if (loggedUser.getAuthority().equals(Authority.ADMIN)) {
                        tilesInfo.put("activeEmployees", getActiveEmployees(loggedUser));
                        tilesInfo.put("activeLocations", getActiveLocations(loggedUser));
                        response.put("invoiceTrends", getPaymentTrends(loggedUser));
                        response.put("employeeTrends", getEmployeeTrends(loggedUser));
                    } else if (loggedUser.getAuthority().equals(Authority.MANAGER)) {
                        tilesInfo.put("activeEmployees", getActiveEmployees(loggedUser));
                        tilesInfo.put("onlineEmployees", getOnlineEmployees(loggedUser));
                        response.put("hoursThisWeek", getHoursThisWeek(loggedUser));
                        response.put("employeeTrends", getEmployeeTrends(loggedUser));
                    } else {
                        tilesInfo.put("hoursWorkedThisWeek", getTotalHoursWorkedThisWeek(loggedUser));
                        tilesInfo.put("nextShift", getNextShift(loggedUser));
                        response.put("hoursThisWeek", getHoursThisWeek(loggedUser));
                    }
                    response.put("tilesInfo", tilesInfo);
                    response.put("weekStart", loggedUser.getOrganization().getFirstDayOfWeek().toString().substring(0, 3));
                    return response;
                })
                .orElseThrow(AuthenticationException::new);
    }

    public PaginationDto getShiftData(int pageNo, int pageSize) {
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                loggedUser.getLocation().getTimezone();
        Instant[] startEnd = DateAndTimeUtils.getTodayInstantRange(timezone);
        if (loggedUser.getAuthority().equals(Authority.EMPLOYEE) || loggedUser.getAuthority().equals(Authority.CONTRACTOR)) {
            startEnd[1] = null;
        }
        String userId = (loggedUser.getAuthority().equals(Authority.EMPLOYEE) || loggedUser.getAuthority().equals(Authority.CONTRACTOR)) ?
                loggedUser.getId() : null;
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize);
        Page<ShiftDetails> shiftsPage = shiftDetailsRepository.getShiftsForDashboard(startEnd[0], startEnd[1],
                loggedUser.getAuthority().equals(Authority.ADMIN), loggedUser.getOrganizationId(), loggedUser.getLocationId(),
                userId, getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(shiftsPage, ShiftsMapper::entityToShiftDto, timezone);
    }

    private Map<String, Object> getPaymentTrends(UserOrganization loggedUser) {
        // TODO Add data
        Map<String, Object> currentYear = new HashMap<>();
        Map<String, Object> previousYear = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        try {

        } catch (Exception e) {
            log.error("Error while getting invoice trends for user {}({})", loggedUser.getFullName(),
                    loggedUser.getId(), e);
        }
        response.put("currentYear", currentYear);
        response.put("previousYear", previousYear);
        response.put("currency", "CAD");
        return response;
    }

    private Map<String, Integer> getActiveEmployees(UserOrganization loggedUser) {
        List<User> users;
        int active;
        int total;
        try {
            if (loggedUser.getAuthority().equals(Authority.ADMIN)) {
                users = userRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false)
                        .stream()
                        .filter(user -> !user.getAuthority().equals(Authority.ADMIN))
                        .collect(Collectors.toList());
            } else {
                users = userRepository.findAllByLocationIdAndDeleteFlag(loggedUser.getLocationId(), false)
                        .stream()
                        .filter(user -> !user.getAuthority().equals(Authority.MANAGER))
                        .collect(Collectors.toList());
            }
            active = (int) users.stream()
                    .filter(user -> user.getAccountStatus().equals(AccountStatus.PAID_AND_ACTIVE)).count();
            total = users.size();
        } catch (Exception e) {
            active = 0;
            total = 0;
            log.error("Error while getting active employees for user {}({})", loggedUser.getFullName(),
                    loggedUser.getId(), e);
        }
        Map<String, Integer> response = new HashMap<>();
        response.put("active", active);
        response.put("total", total);
        return response;
    }

    private int getCurrentWeekHolidays(UserOrganization loggedUser) {
        String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                loggedUser.getLocation().getTimezone();
        Instant[] range = DateAndTimeUtils.getCurrentWeekStartEnd(timezone, DaysOfWeek.SUNDAY);
        String[] dates = new String[]{range[0].toString().split("T")[0], range[1].toString().split("T")[0]};
        int[] date1 = Arrays.stream(dates[0].split("-")).mapToInt(Integer::parseInt).toArray();
        int[] date2 = Arrays.stream(dates[1].split("-")).mapToInt(Integer::parseInt).toArray();
        List<OrganizationHolidays> holidays = new ArrayList<>();
        if (StringUtils.isNotEmpty(loggedUser.getLocationId())) {
            holidays.addAll(organizationHolidayRepository.getAllForLocationByYear(loggedUser.getLocationId(), date1[0]));
            if (date1[0] != date2[0]) {
                holidays.addAll(organizationHolidayRepository.getAllForLocationByYear(loggedUser.getLocationId(), date2[0]));
            }
        }
        if (StringUtils.isEmpty(loggedUser.getLocationId()) || holidays.size() == 0) {
            holidays.addAll(organizationHolidayRepository.getAllForOrganizationByYear(loggedUser.getOrganizationId(), date1[0]));
            if (date1[0] != date2[0]) {
                holidays.addAll(organizationHolidayRepository.getAllForOrganizationByYear(loggedUser.getOrganizationId(), date1[0]));
            }
        }
        return (int) holidays.stream()
                .filter(holiday -> {
                    boolean allowed = holiday.getYear() >= date1[0] || holiday.getYear() <= date2[0];
                    if ((holiday.getYear() == date1[0] && holiday.getMonth() < date1[1]) ||
                            (holiday.getYear() == date2[0] && holiday.getMonth() > date2[1]))
                        allowed = false;
                    if ((holiday.getYear() == date1[0] && holiday.getMonth() == date1[1] &&
                            holiday.getDate() < date1[2]) || (holiday.getYear() == date2[0] &&
                            holiday.getMonth() == date2[1] && holiday.getDate() > date2[2]))
                        allowed = false;
                    return allowed;
                }).count();
    }

    private int getPendingRequests(UserOrganization loggedUser) {
        try {
            if (loggedUser.getAuthority().equals(Authority.ADMIN)) {
                return requestsRepository.findAllByOrganizationIdAndDeleteFlagAndStatusIn(loggedUser.getOrganizationId(),
                                false, Arrays.asList(RequestStatus.NEW, RequestStatus.OPEN))
                        .size();
            } else if (loggedUser.getAuthority().equals(Authority.MANAGER)) {
                return (int) detailedRequestsRepository.findAllByLocation_idAndDeleteFlagAndStatusIn(loggedUser.getLocationId(),
                                false, Arrays.asList(RequestStatus.NEW, RequestStatus.OPEN))
                        .stream().filter(req -> !req.getLocationRole().isAdminRights())
                        .count();
            } else {
                return requestsRepository.findAllByUserIdAndDeleteFlagAndStatusIn(loggedUser.getId(), false,
                                Arrays.asList(RequestStatus.NEW, RequestStatus.OPEN))
                        .size();
            }
        } catch (Exception e) {
            log.error("Error while getting pending requests for user {}({})", loggedUser.getFullName(),
                    loggedUser.getId(), e);
            return 0;
        }
    }

    private int getOnlineEmployees(UserOrganization loggedUser) {
        try {
            return detailedTimesheetRepository.getOnlineEmployeeCount(loggedUser.getOrganizationId(), loggedUser.getLocationId(),
                    loggedUser.getAuthority().equals(Authority.MANAGER));
        } catch (Exception e) {
            log.error("Error while getting online employees for user {}({})", loggedUser.getFullName(),
                    loggedUser.getId(), e);
            return 0;
        }
    }

    private Map<String, Integer> getActiveLocations(UserOrganization loggedUser) {
        int active;
        int total;
        try {
            List<Location> locations = locationRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false);
            active = (int) locations.stream().filter(Location::isActive).count();
            total = locations.size();
        } catch (Exception e) {
            log.error("Error while getting active locations for user {}({})", loggedUser.getFullName(),
                    loggedUser.getId(), e);
            active = 0;
            total = 0;
        }
        Map<String, Integer> response = new HashMap<>();
        response.put("active", active);
        response.put("total", total);
        return response;
    }

    private List<Map<String, Object>> getEmployeeTrends(UserOrganization loggedUser) {
        try {
            if (loggedUser.getAuthority().equals(Authority.ADMIN)) {
                return locationUsersRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false)
                        .stream().map(location -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("location", location.getName());
                            data.put("employees", CommonUtils.getActiveEmployeesFromList(location.getUsers()));
                            return data;
                        }).collect(Collectors.toList());
            } else {
                return locationRoleUsersRepository.findAllByLocationIdAndAdminRightsAndDeleteFlag(loggedUser.getLocationId(),
                                false, false)
                        .stream().map(role -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("role", role.getName());
                            data.put("employees", CommonUtils.getActiveEmployeesFromList(role.getUsers()));
                            return data;
                        }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error while getting employee trends for user {}({})", loggedUser.getFullName(),
                    loggedUser.getId(), e);
            return new ArrayList<>();
        }
    }

    private Map<String, Map<String, Double>> getHoursThisWeek(UserOrganization loggedUser) {
        Instant[] dateRange = DateAndTimeUtils.getCurrentWeekStartEnd(loggedUser.getLocation().getTimezone(),
                loggedUser.getOrganization().getFirstDayOfWeek());
        Instant[] dateRangeLastWeek = new Instant[]{dateRange[0].minus(7, ChronoUnit.DAYS),
                dateRange[1].minus(7, ChronoUnit.DAYS)};
        List<DetailedTimesheet> timesheetList = (loggedUser.getAuthority().equals(Authority.MANAGER) ?
                detailedTimesheetRepository.getByLocationIdAndDates(loggedUser.getLocationId(),
                        dateRangeLastWeek[0], dateRange[1]) :
                detailedTimesheetRepository.getByUserIdAndDates(loggedUser.getId(), dateRangeLastWeek[0], dateRange[1]))
                .stream().filter(timesheet -> timesheet.getEnd() != null)
                .filter(timesheet -> !loggedUser.getAuthority().equals(Authority.MANAGER) || !timesheet.getLocationRole().isAdminRights())
                .collect(Collectors.toList());
        Map<String, Double> currentWeek = timesheetList.stream()
                .filter(timesheet -> !(timesheet.getStart().isBefore(dateRange[0]) || timesheet.getStart().isAfter(dateRange[1])))
                .collect(
                        Collectors.groupingBy(timesheet -> timesheet.getStart().atZone(ZoneId
                                        .of(loggedUser.getLocation().getTimezone())).getDayOfWeek().toString().substring(0, 3),
                                Collectors.summingDouble(timesheet -> timesheet.getStart().until(timesheet.getEnd(), ChronoUnit.MINUTES))
                        )
                );
        Map<String, Double> lastWeek = timesheetList.stream()
                .filter(timesheet -> !(timesheet.getStart().isBefore(dateRangeLastWeek[0]) && timesheet.getStart().isAfter(dateRangeLastWeek[1])))
                .collect(
                        Collectors.groupingBy(timesheet -> timesheet.getStart().atZone(ZoneId
                                        .of(loggedUser.getLocation().getTimezone())).getDayOfWeek().toString().substring(0, 3),
                                Collectors.summingDouble(timesheet -> timesheet.getStart().until(timesheet.getEnd(), ChronoUnit.MINUTES))
                        )
                );
        Map<String, Map<String, Double>> response = new HashMap<>();
        response.put("currentWeek", currentWeek);
        response.put("lastWeek", lastWeek);
        return response;
    }

    public String getTotalHoursWorkedThisWeek(UserOrganization loggedUser) {
        Instant[] dateRange = DateAndTimeUtils.getCurrentWeekStartEnd(loggedUser.getLocation().getTimezone(),
                loggedUser.getOrganization().getFirstDayOfWeek());
        double minutes = timesheetRepository.getAllByUserIdBetweenDates(loggedUser.getId(), dateRange[0], dateRange[1])
                .stream()
                .filter(timesheet -> timesheet.getEnd() != null)
                .mapToDouble(timesheet -> timesheet.getStart().until(timesheet.getEnd(), ChronoUnit.MINUTES))
                .sum();
        return StringUtils.leftPad(String.valueOf((int) (minutes / 60)), 2, "0") + ":" +
                StringUtils.leftPad(String.valueOf((int) (minutes % 60)), 2, "0");
    }

    public Map<String, String> getNextShift(UserOrganization loggedUser) {
        Shifts nextShift = shiftsRepository.findAllByUserIdAndStartAfterOrderByStartAsc(loggedUser.getId(), Instant.now())
                .stream().filter(shift -> shift.getShiftStatus().equals(ShiftStatus.RELEASED)).findFirst().orElse(null);
        Map<String, String> response = new HashMap<>();
        Instant[] todayRange = DateAndTimeUtils.getTodayInstantRange(loggedUser.getLocation().getTimezone());
        Instant[] tomorrowRange = new Instant[]{todayRange[0].plus(1, ChronoUnit.DAYS), todayRange[1].plus(1, ChronoUnit.DAYS)};
        if (nextShift == null) {
            response.put("day", "-");
            response.put("time", "-");
        } else {
            DateTimeDto dateTime = DateAndTimeUtils.getDateTimeObjectWithFullDate(nextShift.getStart(), loggedUser.getLocation().getTimezone());
            response.put("time", dateTime.getTime());
            if (!(nextShift.getStart().isBefore(todayRange[0]) || nextShift.getStart().isAfter(todayRange[1]))) {
                response.put("day", "Today");
            } else if (!(nextShift.getStart().isBefore(tomorrowRange[0]) || nextShift.getStart().isAfter(tomorrowRange[1]))) {
                response.put("day", "Tomorrow");
            } else {
                response.put("day", dateTime.getDate());
            }
        }
        return response;
    }

}