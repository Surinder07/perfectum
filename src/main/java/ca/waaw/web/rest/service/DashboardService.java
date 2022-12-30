package ca.waaw.web.rest.service;

import ca.waaw.domain.Location;
import ca.waaw.domain.User;
import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.Authority;
import ca.waaw.repository.LocationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.joined.LocationUsersRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DashboardService {

    private final Logger log = LogManager.getLogger(DashboardService.class);

    private final UserRepository userRepository;

    private final LocationRepository locationRepository;

    private final LocationUsersRepository locationUsersRepository;

    public Map<String, Object> getDashboardData() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    Map<String, Object> response = new HashMap<>();
                    Map<String, Object> tilesInfo = new HashMap<>();
                    tilesInfo.put("holidayCurrentWeek", getCurrentWeekHolidays(loggedUser));
                    tilesInfo.put("pendingRequests", getPendingRequests(loggedUser));
                    if (loggedUser.getAuthority().equals(Authority.ADMIN)) {
                        tilesInfo.put("activeEmployees", getActiveEmployees(loggedUser));
                        tilesInfo.put("activeLocations", getActiveLocations(loggedUser));
                        response.put("invoiceTrends", getInvoiceTrends(loggedUser));
                        response.put("employeeTrends", getEmployeeTrends(loggedUser));
                    } else if (loggedUser.getAuthority().equals(Authority.MANAGER)) {
//                        tilesInfo.put("activeEmployees", getActiveEmployees());
//                        tilesInfo.put("onlineEmployees", getOnlineEmployees());
//                        response.put("shiftsToday", PaginationDto.builder().data(new ArrayList<>()).totalEntries(0).totalPages(0).build());
                        response.put("hoursThisWeek", getHoursThisWeek(loggedUser));
                        response.put("employeeTrends", getEmployeeTrends(loggedUser));
                    } else {
//                        response.put("hoursWorkedByWeek", getHoursWorkedByWeek());
//                        response.put("nextShift", getNextShift());
                        response.put("timeSheets", PaginationDto.builder().data(new ArrayList<>()).totalEntries(0).totalPages(0).build());
                        response.put("hoursThisWeek", getHoursThisWeek(loggedUser));
                    }
                    response.put("tilesInfo", tilesInfo);
                    return response;
                })
                .orElseThrow(AuthenticationException::new);
    }

    private Map<String, Object> getInvoiceTrends(User loggedUser) {
        Map<String, Object> currentYear = new HashMap<>();
        currentYear.put("Jan", 500);
        currentYear.put("Feb", 480);
        currentYear.put("Mar", 500);
        currentYear.put("Apr", 560);
        currentYear.put("May", 560);
        currentYear.put("Jun", 600);
        Map<String, Object> previousYear = new HashMap<>();
        previousYear.put("Jan", 400);
        previousYear.put("Feb", 420);
        previousYear.put("Mar", 400);
        previousYear.put("Apr", 500);
        previousYear.put("May", 470);
        previousYear.put("Jun", 450);
        previousYear.put("Jul", 410);
        previousYear.put("Aug", 380);
        previousYear.put("Sep", 420);
        previousYear.put("Oct", 400);
        previousYear.put("Nov", 400);
        previousYear.put("Dec", 300);
        Map<String, Object> response = new HashMap<>();
        response.put("currentYear", currentYear);
        response.put("previousYear", previousYear);
        response.put("currency", "CAD");
        return response;
    }

    private int getCurrentWeekHolidays(User loggedUser) {
        return 0;
    }

    private int getPendingRequests(User loggedUser) {
        return 0;
    }

    private String getActiveEmployees(User loggedUser) {
        List<User> users = userRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false);
        int active = (int) users.stream().map(user -> user.getAccountStatus().equals(AccountStatus.PAID_AND_ACTIVE)).count();
        int total = users.size();
        return active + "/" + total;
    }

    private String getActiveLocations(User loggedUser) {
        List<Location> locations = locationRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false);
        int active = (int) locations.stream().map(Location::isActive).count();
        int total = locations.size();
        return active + "/" + total;
    }

    private List<Map<String, Object>> getEmployeeTrends(User loggedUser) {
        return locationUsersRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false)
                .stream().map(location -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("location", location.getName());
                    data.put("employees", CommonUtils.getActiveEmployeesFromList(location.getUsers()));
                    return data;
                }).collect(Collectors.toList());
    }

    private Map<String, Object> getHoursThisWeek(User loggedUser) {
        return null;
    }

}