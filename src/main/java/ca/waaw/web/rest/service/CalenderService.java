package ca.waaw.web.rest.service;

import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.timesheet.Timesheet;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.calender.EventsDto;
import ca.waaw.dto.calender.TimesheetDto;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.enumration.request.RequestType;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.repository.requests.RequestsRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.timesheet.TimesheetRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CalenderService {

    private final UserOrganizationRepository userRepository;

    private final TimesheetRepository timesheetRepository;

    private final RequestsRepository requestsRepository;

    private final ShiftsRepository shiftsRepository;

    private final Logger log = LogManager.getLogger(CalenderService.class);

    public List<TimesheetDto> getTimesheet(int month, int year) {
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone();
        Instant[] dates = DateAndTimeUtils.getMonthStartEnd(year, month, timezone);
        List<Timesheet> timesheetList = timesheetRepository.getAllByUserIdBetweenDates(loggedUser.getId(), dates[0], dates[1]);
        return timesheetList.stream()
                .map(timesheet -> {
                    TimesheetDto dto = new TimesheetDto();
                    dto.setStartDatetime(DateAndTimeUtils.getInstantAsStringInGivenTimezone(timesheet.getStart(), timezone));
                    if (timesheet.getEnd() != null)
                        dto.setEndDatetime(DateAndTimeUtils.getInstantAsStringInGivenTimezone(timesheet.getEnd(), timezone));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<EventsDto> getDayEvents(String date) {
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone();
        Instant[] dates = DateAndTimeUtils.getStartAndEndTimeForInstant(date, timezone);
        List<EventsDto> response = new ArrayList<>();
        if (loggedUser.getAuthority().equals(Authority.ADMIN)) {
            int shiftsAssigned = shiftsRepository.findAllByOrganizationIdAndStartBetweenAndDeleteFlag(loggedUser.getOrganizationId(), dates[0], dates[1], false)
                    .stream().map(Shifts::getUserId).collect(Collectors.toSet()).size();
            int totalEmployees = (int) userRepository.findAllByOrganizationIdAndDeleteFlag(loggedUser.getOrganizationId(), false)
                    .stream().filter(emp -> !emp.getAuthority().equals(Authority.ADMIN)).count();
            response.add(EventsDto.builder().name("Shifts Assigned for")
                    .time(shiftsAssigned + "/" + totalEmployees + " Employees").build());
        } else {
            List<Shifts> shifts = shiftsRepository.findAllByUserIdAndStartBetweenAndDeleteFlag(loggedUser.getId(), dates[0], dates[1], false);
            shifts.stream()
                    .filter(shift -> shift.getShiftStatus().equals(ShiftStatus.RELEASED))
                    .forEach(shift -> {
                        String start = DateAndTimeUtils.getDateTimeObject(shift.getStart(), timezone).getTime();
                        String end = DateAndTimeUtils.getDateTimeObject(shift.getEnd(), timezone).getTime();
                        response.add(EventsDto.builder()
                                .name("My Shift").time(start + " - " + end)
                                .build());
                    });
            requestsRepository.getOneForUserBetweenDatesAndByType(loggedUser.getId(), RequestType.TIME_OFF, dates[0], dates[1])
                    .ifPresent(request -> {
                        if (request.getStatus().equals(RequestStatus.ACCEPTED))
                            response.add(EventsDto.builder()
                                    .name("Timeoff")
                                    .time((request.getSubType().equals(RequestSubType.SICK_LEAVE_FULL_DAY) || request.getSubType().equals(RequestSubType.VACATION_LEAVE_FULL_DAY)) ?
                                            "Full Day" : DateAndTimeUtils.getDateTimeObject(request.getStart(), timezone).getTime()
                                            + " - " + DateAndTimeUtils.getDateTimeObject(request.getEnd(), timezone).getTime())
                                    .build());
                    });
        }
        return response;
    }

}