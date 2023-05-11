package ca.waaw.web.rest.service;

import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.timesheet.Timesheet;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.timesheet.TimesheetDto;
import ca.waaw.dto.timesheet.ActiveTimesheetDto;
import ca.waaw.dto.timesheet.TimesheetDetailsDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.mapper.TimesheetMapper;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.timesheet.TimesheetRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.application.ActiveTimesheetPresentException;
import ca.waaw.web.rest.errors.exceptions.application.TimesheetOverlappingException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final UserRepository userRepository;

    private final ShiftsRepository shiftsRepository;

    /**
     * Start timesheet recording for logged-in user
     */
    public void startTimesheetRecording() {
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(loggedUser -> {
                            timesheetRepository.getActiveTimesheet(loggedUser.getId())
                                    .ifPresent(timesheet -> {
                                        throw new ActiveTimesheetPresentException();
                                    });
                            return loggedUser;
                        })
                        .map(loggedUser -> {
                            Instant start = Instant.now().plus(loggedUser.getOrganization().getClockInAllowedMinutesBeforeShift(),
                                    ChronoUnit.MINUTES).plus(1L, ChronoUnit.SECONDS);
                            String shiftId = shiftsRepository.getAllUpcomingOrOngoingShifts(loggedUser.getId(), start, Instant.now())
                                    .map(Shifts::getId)
                                    .orElseThrow(() -> new EntityNotFoundException("shift"));
                            return TimesheetMapper.createNewEntityForLoggedInUser(loggedUser, shiftId);
                        })
                )
                .map(timesheetRepository::save)
                .map(timesheet -> CommonUtils.logMessageAndReturnObject(timesheet, "info", TimesheetService.class,
                        "New timesheet recording started: {}", timesheet));
    }

    /**
     * Stop timesheet recording for logged-in user
     */
    public void stopTimesheetRecording() {
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(loggedUser -> timesheetRepository.getActiveTimesheet(loggedUser.getId())
                                .map(timesheet -> {
                                    timesheet.setEnd(Instant.now());
                                    return timesheet;
                                })
                        )
                        .orElseThrow(() -> new EntityNotFoundException("Active timer"))
                )
                .map(timesheetRepository::save)
                .map(timesheet -> CommonUtils.logMessageAndReturnObject(timesheet, "info", TimesheetService.class,
                        "Timesheet recording stopped: {}", timesheet));
    }

    /**
     * @return Date time info for timer start if active timer is present
     */
    public ActiveTimesheetDto getActiveTimesheet() {
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE, Authority.MANAGER);
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        ActiveTimesheetDto response = timesheetRepository.getActiveTimesheet(loggedUser.getId())
                .map(timesheet -> mapActiveTimesheet(timesheet, loggedUser.getLocation().getTimezone()))
                .orElse(new ActiveTimesheetDto());
        Instant start = Instant.now().plus(loggedUser.getOrganization().getClockInAllowedMinutesBeforeShift(),
                ChronoUnit.MINUTES).plus(1L, ChronoUnit.SECONDS).plus(5L, ChronoUnit.MINUTES);
        shiftsRepository.getAllUpcomingOrOngoingShifts(loggedUser.getId(), start, Instant.now())
                .ifPresent(shift -> {
                    response.setUpcomingShift(true);
                    int timeRemaining = (int) Duration.between(Instant.now(), shift.getStart()).toSeconds();
                    timeRemaining = timeRemaining - (loggedUser.getOrganization().getClockInAllowedMinutesBeforeShift() * 60);
                    if (timeRemaining < 0) timeRemaining = 0;
                    response.setShiftsAfterSeconds(timeRemaining);
                });
        Instant[] todayRange = DateAndTimeUtils.getTodayInstantRange(loggedUser.getAuthority().equals(Authority.ADMIN) ?
                loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone());
        int timeToday = timesheetRepository.getAllByUserIdBetweenDates(loggedUser.getId(), todayRange[0], todayRange[1])
                .stream()
                .filter(timesheet -> timesheet.getEnd() != null)
                .mapToInt(timesheet -> (int) Duration.between(timesheet.getStart(), timesheet.getEnd()).toSeconds())
                .sum();
        response.setTotalTimeWorkedToday(timeToday);
        return response;
    }

    private ActiveTimesheetDto mapActiveTimesheet(Timesheet source, String timezone) {
        ActiveTimesheetDto target = new ActiveTimesheetDto();
        DateTimeDto start = DateAndTimeUtils.getDateTimeObject(source.getStart(), timezone);
        target.setStartTime(start.getTime());
        target.setStartDate(start.getDate());
        target.setStartTimestamp(source.getStart());
        if (source.getEnd() != null) {
            DateTimeDto end = DateAndTimeUtils.getDateTimeObject(source.getEnd(), timezone);
            target.setEndTime(end.getTime());
            target.setEndDate(end.getDate());
            target.setEndTimestamp(source.getEnd());
        }
        return target;
    }

    /**
     * @param pageNo    page number for pagination
     * @param pageSize  page size for pagination
     * @param startDate start date for range
     * @param endDate   end date for range
     * @param type      type of added timesheet
     * @return pagination list of all time sheets
     */
    // todo add shift info in response
    public PaginationDto getAllTimeSheet(int pageNo, int pageSize, String startDate, String endDate, String type, String userId) {
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                loggedUser.getLocation().getTimezone();
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        if (loggedUser.getAuthority().equals(Authority.ADMIN) && userId == null)
            throw new BadRequestException("userId is required");
        else if (loggedUser.getAuthority().equals(Authority.MANAGER) && userId == null) userId = loggedUser.getId();
        else if (!loggedUser.getAuthority().equals(Authority.ADMIN)) userId = loggedUser.getId();
        Instant[] dateRange = startDate == null ? new Instant[]{null, null} :
                DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone);
        Page<Timesheet> timesheetPage = timesheetRepository.filterTimesheet(userId, dateRange[0],
                dateRange[1], type, getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(timesheetPage, TimesheetMapper::entityToDetailedDto, timezone);
    }

    public TimesheetDetailsDto getTimeSheetsById(String id) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> timesheetRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(timesheet -> {
                            if ((loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                    loggedUser.getLocationId().equals(timesheet.getLocationId())) ||
                                    !loggedUser.getOrganizationId().equals(timesheet.getOrganizationId())) {
                                return null;
                            }
                            String timezone = loggedUser.getAuthority().equals(Authority.MANAGER) ?
                                    loggedUser.getLocation().getTimezone() : loggedUser.getOrganization().getTimezone();
                            return TimesheetMapper.entityToDetailedDto(timesheet, timezone);
                        }).orElseThrow(() -> new EntityNotFoundException("timesheet"))
                ).orElseThrow(AuthenticationException::new);
    }

    // Add timesheet (admin) TODO
    public void addNewTimesheet(TimesheetDto timesheetDto) {
//        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
//        if (StringUtils.isEmpty(timesheetDto.getUserId()))
//            throw new BadRequestException("Missing a required value.", "userId");
//        SecurityUtils.getCurrentUserLogin()
//                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
//                .map(loggedUser -> userOrganizationRepository.findOneByIdAndDeleteFlag(timesheetDto.getUserId(), false)
//                        .map(user -> {
//                            Instant start = DateAndTimeUtils.getDateInstant(timesheetDto.getStart().getDate(),
//                                    timesheetDto.getStart().getTime(), user.getLocation().getTimezone());
//                            Instant end = DateAndTimeUtils.getDateInstant(timesheetDto.getEnd().getDate(),
//                                    timesheetDto.getEnd().getTime(), user.getLocation().getTimezone());
//                            timesheetRepository.getByUserIdBetweenDates(user.getId(), start, end)
//                                    .ifPresent(timesheet -> {
//                                        throw new TimesheetOverlappingException();
//                                    });
//                            return TimesheetMapper.dtoToEntity(loggedUser.getId(), start, end);
//                        })
//                        .map(timesheetRepository::save)
//                        .map(timesheet -> CommonUtils.logMessageAndReturnObject(timesheet, "info", TimesheetService.class,
//                                "Timesheet edited successfully: {}", timesheet))
//                );
    }

    /**
     * @param timesheetDto timesheet details to be edited
     */
    public void editTimesheet(TimesheetDto timesheetDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        if (StringUtils.isEmpty(timesheetDto.getId()))
            throw new BadRequestException("Missing a required value.", "id");
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> timesheetRepository.findOneByIdAndDeleteFlag(timesheetDto.getId(), false)
                        .flatMap(timesheet -> userOrganizationRepository.findOneByIdAndDeleteFlag(timesheet.getUserId(), false)
                                .map(user -> {
                                    if (!loggedUser.getOrganizationId().equals(user.getOrganizationId()) ||
                                            (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                                    (!loggedUser.getLocationId().equals(user.getLocationId()) ||
                                                            user.getLocationRole().isAdminRights()))) return null;
                                    Instant start = DateAndTimeUtils.getDateInstant(timesheetDto.getStart().getDate(),
                                            timesheetDto.getStart().getTime(), user.getLocation().getTimezone());
                                    Instant end = DateAndTimeUtils.getDateInstant(timesheetDto.getEnd().getDate(),
                                            timesheetDto.getEnd().getTime(), user.getLocation().getTimezone());
                                    timesheetRepository.getByUserIdBetweenDates(user.getId(), start, end)
                                            .ifPresent(timesheet1 -> {
                                                if (!timesheet.equals(timesheet1)) {
                                                    throw new TimesheetOverlappingException();
                                                }
                                            });
                                    timesheet.setStart(start);
                                    timesheet.setEnd(end);
                                    timesheet.setLastModifiedBy(loggedUser.getId());
                                    timesheet.setComment(timesheetDto.getComments());
                                    return timesheet;
                                })
                                .map(timesheetRepository::save)

                        )
                        .orElseThrow(() -> new EntityNotFoundException("timesheet"))
                )
                .map(timesheet -> CommonUtils.logMessageAndReturnObject(timesheet, "info", TimesheetService.class,
                        "Timesheet edited successfully: {}", timesheet));
    }

    /**
     * @param id id for the timesheet to be deleted
     */
    public void deleteTimesheet(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> timesheetRepository.findOneByUserIdAndDeleteFlag(id, false)
                        .map(timesheet -> {
                            userRepository.findOneByIdAndDeleteFlag(timesheet.getUserId(), false)
                                    .map(user -> {
                                        if (!user.getOrganizationId().equals(loggedUser.getOrganizationId()) ||
                                                (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                                        !loggedUser.getLocationId().equals(user.getLocationId()))) {
                                            return null;
                                        }
                                        return user;
                                    })
                                    .orElseThrow(() -> new EntityNotFoundException("timesheet"));
                            timesheet.setDeleteFlag(true);
                            timesheet.setLastModifiedBy(loggedUser.getId());
                            return timesheet;
                        })
                        .map(timesheetRepository::save)
                        .map(timesheet -> CommonUtils.logMessageAndReturnObject(timesheet, "info", TimesheetService.class,
                                "Timesheet deleted successfully: {}", timesheet))
                        .orElseThrow(() -> new EntityNotFoundException("timesheet"))
                );
    }

}