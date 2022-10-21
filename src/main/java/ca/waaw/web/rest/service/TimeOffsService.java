package ca.waaw.web.rest.service;

import ca.waaw.domain.Shifts;
import ca.waaw.domain.TimeOffs;
import ca.waaw.domain.joined.DetailedTimeOff;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.timeoff.NewTimeOffDto;
import ca.waaw.dto.timeoff.TimeOffInfoDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.mapper.TimeoffMapper;
import ca.waaw.repository.DetailedTimeOffRepository;
import ca.waaw.repository.ShiftsRepository;
import ca.waaw.repository.TimeOffsRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

@Service
@AllArgsConstructor
public class TimeOffsService {

    private final TimeOffsRepository timeOffsRepository;

    private final DetailedTimeOffRepository detailedTimeOffRepository;

    private final ShiftsRepository shiftsRepository;

    private final UserOrganizationRepository userRepository;

    /**
     * TODO
     *
     * @param pageNo    page number starting from 0
     * @param pageSize  number of items to be shown
     * @param showAll   if all requests are to be shown
     * @param startDate if any range of requests is needed, send start date and end date
     * @param endDate   end date for the range
     * @return pagination list for all time offs
     */
    public PaginationDto getAllTimeOff(int pageNo, int pageSize, boolean showAll, String startDate, String endDate) {
        if (StringUtils.isEmpty(startDate) && StringUtils.isNotEmpty(endDate) ||
                StringUtils.isNotEmpty(startDate) && StringUtils.isEmpty(endDate)) {
            throw new BadRequestException("Pass both start and end date to filter for dates.", "startDate", "endDate");
        }
        boolean returnRange = StringUtils.isNotEmpty(startDate);
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdTime").descending());
        AtomicReference<String> timezone = new AtomicReference<>(null);
        Page<DetailedTimeOff> timeoffPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedInUser -> {
                    Instant start = Instant.now();
                    Instant end = null;
                    timezone.set(loggedInUser.getAuthority().equals(Authority.ADMIN) ? loggedInUser.getOrganization().getTimezone() :
                            loggedInUser.getLocation().getTimezone());
                    if (returnRange) {
                        start = DateAndTimeUtils.getDateInstant(startDate, "00:00:00", timezone.get());
                        end = DateAndTimeUtils.getDateInstant(startDate, "23:59:59", timezone.get());
                    }
                    if (loggedInUser.getAuthority().equals(Authority.ADMIN)) {
                        return returnRange ? detailedTimeOffRepository.getByOrganizationIdBetweenDates(loggedInUser.getOrganizationId(),
                                start, end, showAll ? null : EntityStatus.PENDING, getSortedByCreatedDate) :
                                detailedTimeOffRepository.getByOrganizationIdAfterDate(loggedInUser.getOrganizationId(),
                                        start, showAll ? null : EntityStatus.PENDING, getSortedByCreatedDate);
                    } else if (loggedInUser.getAuthority().equals(Authority.MANAGER)) {
                        return returnRange ? detailedTimeOffRepository.getByLocationIdBetweenDates(loggedInUser.getLocationId(),
                                start, end, showAll ? null : EntityStatus.PENDING, getSortedByCreatedDate) :
                                detailedTimeOffRepository.getByLocationIdAfterDate(loggedInUser.getLocationId(),
                                        start, showAll ? null : EntityStatus.PENDING, getSortedByCreatedDate);
                    } else {
                        return returnRange ? detailedTimeOffRepository.getByUserIdBetweenDates(loggedInUser.getId(),
                                start, end, showAll ? null : EntityStatus.PENDING, getSortedByCreatedDate) :
                                detailedTimeOffRepository.getByUserIdAfterDate(loggedInUser.getId(),
                                        start, showAll ? null : EntityStatus.PENDING, getSortedByCreatedDate);
                    }
                })
                .orElseThrow(UnauthorizedException::new);
        BiFunction<DetailedTimeOff, String, TimeOffInfoDto> mapper = TimeoffMapper::entityToDto;
        return CommonUtils.getPaginationResponse(timeoffPage, mapper, timezone.get());
    }

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