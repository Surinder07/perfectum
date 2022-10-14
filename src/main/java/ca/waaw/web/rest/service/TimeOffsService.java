package ca.waaw.web.rest.service;

import ca.waaw.domain.Shifts;
import ca.waaw.domain.TimeOffs;
import ca.waaw.dto.NewTimeOffDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.mapper.TimeoffMapper;
import ca.waaw.repository.ShiftsRepository;
import ca.waaw.repository.TimeOffsRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TimeOffsService {

    private final TimeOffsRepository timeOffsRepository;

    private final ShiftsRepository shiftsRepository;

    private final UserOrganizationRepository userRepository;

    // See all timeoff requests with extra option to see allowed ones for admin TODO

    /**
     * Create a new timeoff request for employees or let the admin add a timeoff directly.
     *
     * @param newTimeOffDto New timeoff details
     */
    public void addNewTimeoff(NewTimeOffDto newTimeOffDto) {
        if (newTimeOffDto.getStartDate().getTime() == null || newTimeOffDto.getEndDate().getTime() == null) {
            newTimeOffDto.getStartDate().setTime("00:00:00");
            newTimeOffDto.getEndDate().setTime("23:59:59");
        }
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(loggedInUser -> {
                            TimeOffs timeOff;
                            if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN, Authority.MANAGER)) {
                                timeOff = userRepository.findOneByIdAndDeleteFlag(newTimeOffDto.getUserId(), false)
                                        .map(user -> {
                                            if (!loggedInUser.getOrganizationId().equals(user.getOrganizationId()) ||
                                                    loggedInUser.getAuthority().equals(Authority.MANAGER) &&
                                                            !loggedInUser.getLocationId().equals(user.getLocationId())) {
                                                return null;
                                            }
                                            checkOverlappingAndPastTimeoff(newTimeOffDto, user.getLocation().getTimezone(), false);
                                            checkAndUpdateShiftsIfPresent(newTimeOffDto, user.getLocation().getTimezone(), loggedInUser.getId());
                                            return TimeoffMapper.dtoToEntity(newTimeOffDto, user.getLocation().getTimezone());
                                        })
                                        .orElseThrow(() -> new EntityNotFoundException("user"));
                                timeOff.setStatus(EntityStatus.ACTIVE);
                            } else {
                                newTimeOffDto.setUserId(loggedInUser.getId());
                                checkOverlappingAndPastTimeoff(newTimeOffDto, loggedInUser.getLocation().getTimezone(), true);
                                timeOff = TimeoffMapper.dtoToEntity(newTimeOffDto, loggedInUser.getLocation().getTimezone());
                                timeOff.setStatus(EntityStatus.PENDING);
                            }
                            timeOff.setCreatedBy(loggedInUser.getId());
                            return timeOff;
                        })
                )
                .map(timeOffsRepository::save)
                .map(timeOff -> CommonUtils.logMessageAndReturnObject(timeOff, "info", TimeOffsService.class,
                        "New timeoff added successfully {}", timeOff));
        // TODO send notification to admin for request or to employee for direct addition
    }

    /**
     * @param id     id to update status of
     * @param accept true to accept and false for reject
     */
    public void respondToRequest(String id, boolean accept) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> timeOffsRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(timeOff -> userRepository.findOneByIdAndDeleteFlag(timeOff.getUserId(), false)
                                .map(user -> {
                                    if (!user.getOrganizationId().equals(admin.getOrganizationId()) ||
                                            (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                                    !user.getLocationId().equals(admin.getLocationId()))) {
                                        return null;
                                    }
                                    timeOff.setStatus(accept ? EntityStatus.ACTIVE : EntityStatus.REJECTED);
                                    timeOff.setLastModifiedBy(admin.getId());
                                    return timeOff;
                                })
                                .orElseThrow(() -> new EntityNotFoundException("user"))
                        )
                        .orElseThrow(() -> new EntityNotFoundException("timeoff request"))
                )
                .map(timeoff -> CommonUtils.logMessageAndReturnObject(timeoff, "info", TimeOffsService.class,
                        "Timeoff status updated to {} for id {}", accept ? EntityStatus.ACTIVE : EntityStatus.REJECTED, id))
                .orElseThrow(UnauthorizedException::new);
        // TODO notify to employee
    }

    /**
     * @param id id for the request to be deleted
     */
    public void deleteTimeoff(String id) {
        CommonUtils.checkRoleAuthorization(Authority.EMPLOYEE);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> timeOffsRepository.findOneByIdAndDeleteFlag(id, false)
                        .filter(timeOff -> timeOff.getStatus().equals(EntityStatus.PENDING))
                        .map(timeoff -> {
                            if (!timeoff.getUserId().equals(user.getId())) return null;
                            timeoff.setDeleteFlag(true);
                            timeoff.setLastModifiedBy(user.getId());
                            return timeoff;
                        })
                        .map(timeOffsRepository::save)
                        .orElseThrow(() -> new EntityNotFoundException("Timeoff Request"))
                )
                .map(timeOff -> CommonUtils.logMessageAndReturnObject(timeOff, "info", TimeOffsService.class,
                        "Timeoff request deleted successfully: {}", timeOff))
                .orElseThrow(UnauthorizedException::new);
    }

    // Edit timeoff request TODO

    private void checkOverlappingAndPastTimeoff(NewTimeOffDto dto, String timezone, boolean checkPast) {
        Instant start = DateAndTimeUtils.getDateInstant(dto.getStartDate().getDate(),
                dto.getStartDate().getTime(), timezone);
        Instant end = DateAndTimeUtils.getDateInstant(dto.getEndDate().getDate(),
                dto.getEndDate().getTime(), timezone);
        timeOffsRepository.getByUserIdBetweenDates(dto.getUserId(), start, end)
                .ifPresent(timeOff -> {
                    throw new EntityAlreadyExistsException("timeOff", timeOff.getStartDate() + " - "
                            + timeOff.getEndDate());
                });
        if (checkPast && Instant.now().isAfter(start)) {
            throw new PastValueNotDeletableException("timeoff");
        }
    }

    private void checkAndUpdateShiftsIfPresent(NewTimeOffDto dto, String timezone, String loggedUser) {
        Instant start = DateAndTimeUtils.getDateInstant(dto.getStartDate().getDate(),
                dto.getStartDate().getTime(), timezone);
        Instant end = DateAndTimeUtils.getDateInstant(dto.getEndDate().getDate(),
                dto.getEndDate().getTime(), timezone);
        List<Shifts> shifts = shiftsRepository.getByUserIdBetweenDates(dto.getUserId(), start, end);
        shifts.forEach(shift -> {
            shift.setConflict(true);
            shift.setConflictReason("Timeoff accepted");
            shift.setLastModifiedBy(loggedUser);
            if (shift.getStart().isBefore(start) || shift.getEnd().isAfter(end)) {
                Shifts newShift = new Shifts();
                BeanUtils.copyProperties(shift, newShift);
                newShift.setId(UUID.randomUUID().toString());
                newShift.setCreatedBy(loggedUser);
                newShift.setCreatedDate(Instant.now());
                if (shift.getStart().isBefore(start)) newShift.setEnd(start);
                else newShift.setStart(end);
                shifts.add(newShift);
            }
        });
        if (shifts.size() > 0) {
            shiftsRepository.saveAll(shifts);
            //TODO Send notification to admin
        }
    }

}