package ca.waaw.web.rest.service;

import ca.waaw.domain.Timesheet;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.TimesheetDto;
import ca.waaw.dto.timesheet.ActiveTimesheetDto;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.TimesheetMapper;
import ca.waaw.repository.TimesheetRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.joined.DetailedTimesheetRepository;
import ca.waaw.repository.joined.UserOrganizationRepository;
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

import java.time.Instant;

@Service
@AllArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;

    private final DetailedTimesheetRepository detailedTimesheetRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final UserRepository userRepository;

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
                        .map(TimesheetMapper::createNewEntityForLoggedInUser)
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
                                    timesheet.setEnd(DateAndTimeUtils.getCurrentDateTime(loggedUser.getLocation().getTimezone()));
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
        Instant[] todayRange = DateAndTimeUtils.getStartAndEndTimeForInstant(Instant.now());
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        ActiveTimesheetDto response = timesheetRepository.getActiveTimesheet(loggedUser.getId())
                .map(timesheet -> mapActiveTimesheet(timesheet, loggedUser.getLocation().getTimezone()))
                .orElse(null);
        if (response == null) {
            response = timesheetRepository.getByUserIdBetweenDates(loggedUser.getId(), todayRange[0], todayRange[1])
                    .map(timesheet -> mapActiveTimesheet(timesheet, loggedUser.getLocation().getTimezone()))
                    .orElse(null);
        }
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

    // Add timesheet (admin)
    public void addNewTimesheet(TimesheetDto timesheetDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        if (StringUtils.isEmpty(timesheetDto.getUserId()))
            throw new BadRequestException("Missing a required value.", "userId");
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> userOrganizationRepository.findOneByIdAndDeleteFlag(timesheetDto.getUserId(), false)
                        .map(user -> {
                            Instant start = DateAndTimeUtils.getDateInstant(timesheetDto.getStart().getDate(),
                                    timesheetDto.getStart().getTime(), user.getLocation().getTimezone());
                            Instant end = DateAndTimeUtils.getDateInstant(timesheetDto.getEnd().getDate(),
                                    timesheetDto.getEnd().getTime(), user.getLocation().getTimezone());
                            timesheetRepository.getByUserIdBetweenDates(user.getId(), start, end)
                                    .ifPresent(timesheet -> {
                                        throw new TimesheetOverlappingException();
                                    });
                            return TimesheetMapper.dtoToEntity(loggedUser.getId(), start, end);
                        })
                        .map(timesheetRepository::save)
                        .map(timesheet -> CommonUtils.logMessageAndReturnObject(timesheet, "info", TimesheetService.class,
                                "Timesheet edited successfully: {}", timesheet))
                );
    }

    /**
     * @param timesheetDto timesheet details to be edited
     */
    public void editTimesheet(TimesheetDto timesheetDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        if (StringUtils.isEmpty(timesheetDto.getUserId()))
            throw new BadRequestException("Missing a required value.", "id");
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> timesheetRepository.findOneByIdAndDeleteFlag(timesheetDto.getId(), false)
                        .flatMap(timesheet -> userOrganizationRepository.findOneByIdAndDeleteFlag(timesheet.getUserId(), false)
                                .map(user -> {
                                    Instant start = DateAndTimeUtils.getDateInstant(timesheetDto.getStart().getDate(),
                                            timesheetDto.getStart().getTime(), user.getLocation().getTimezone());
                                    Instant end = DateAndTimeUtils.getDateInstant(timesheetDto.getEnd().getDate(),
                                            timesheetDto.getEnd().getTime(), user.getLocation().getTimezone());
                                    timesheetRepository.getByUserIdBetweenDates(user.getId(), start, end)
                                            .ifPresent(timesheet1 -> {
                                                if (!(timesheet.getStart().equals(timesheet1.getStart()) &&
                                                        timesheet.getEnd().equals(timesheet1.getEnd()))) {
                                                    throw new TimesheetOverlappingException();
                                                }
                                            });
                                    timesheet.setStart(start);
                                    timesheet.setEnd(end);
                                    timesheet.setLastModifiedBy(loggedUser.getId());
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