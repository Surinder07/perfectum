package ca.waaw.web.rest.service;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.TimesheetDetailDto;
import ca.waaw.dto.TimesheetDto;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.TimesheetMapper;
import ca.waaw.repository.DetailedTimesheetRepository;
import ca.waaw.repository.TimesheetRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.ActiveTimesheetPresentException;
import ca.waaw.web.rest.errors.exceptions.application.TimesheetOverlappingException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE);
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
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE);
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
    public DateTimeDto getActiveTimesheet() {
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .flatMap(loggedUser -> timesheetRepository.getActiveTimesheet(loggedUser.getId())
                                .map(timesheet -> DateAndTimeUtils.getDateTimeObject(timesheet.getStart(),
                                        loggedUser.getLocation().getTimezone()))
                        )
                ).orElse(null);
    }

    /**
     * @param startDate start date for range
     * @param endDate   end date for range
     * @return list of all time sheets
     */
    public List<TimesheetDetailDto> getAllTimeSheet(String startDate, String endDate) {
        AtomicReference<String> timezone = new AtomicReference<>(null);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    timezone.set(loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                            loggedUser.getLocation().getTimezone());
                    Instant start = DateAndTimeUtils.getDateInstant(startDate, "00:00:00", timezone.get());
                    Instant end = DateAndTimeUtils.getDateInstant(endDate, "23:59:59", timezone.get());
                    if (loggedUser.getAuthority().equals(Authority.ADMIN))
                        return detailedTimesheetRepository.getByOrganizationIdAndDates(loggedUser.getOrganizationId(),
                                start, end);
                    else if (loggedUser.getAuthority().equals(Authority.MANAGER))
                        return detailedTimesheetRepository.getByLocationIdAndDates(loggedUser.getLocationId(),
                                start, end);
                    else
                        return detailedTimesheetRepository.getByUserIdAndDates(loggedUser.getId(),
                                start, end);
                })
                .map(timeSheets -> timeSheets.stream()
                        .map(timesheet -> TimesheetMapper.entityToDto(timesheet, timezone.get()))
                        .collect(Collectors.toList())
                )
                .orElseThrow(UnauthorizedException::new);
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