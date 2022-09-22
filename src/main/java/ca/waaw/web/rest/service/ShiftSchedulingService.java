package ca.waaw.web.rest.service;

import ca.waaw.domain.Shifts;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.repository.LocationRepository;
import ca.waaw.repository.LocationRoleRepository;
import ca.waaw.repository.ShiftsRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.ShiftSchedulingUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class ShiftSchedulingService {

    private final Logger log = LogManager.getLogger(ShiftSchedulingService.class);

    private final ShiftsRepository shiftsRepository;

    private final UserOrganizationRepository userRepository;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    /**
     * Create a new Shift
     *
     * @param newShiftDto New shift details
     */
    public void createShift(NewShiftDto newShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        ShiftSchedulingPreferences shiftSchedulingPreferences = new ShiftSchedulingPreferences();
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(user -> {
                            if (user.getLocationRole() != null) {
                                shiftSchedulingPreferences.setMinHoursBetweenShifts(user.getLocationRole().getMinHoursBetweenShifts());
                                shiftSchedulingPreferences.setMaxConsecutiveWorkDays(user.getLocationRole().getMaxConsecutiveWorkDays());
                                shiftSchedulingPreferences.setTotalHoursPerDayMax(user.getLocationRole().getTotalHoursPerDayMax());
                                shiftSchedulingPreferences.setTotalHoursPerDayMin(user.getLocationRole().getTotalHoursPerDayMin());
                            }
                            return user;
                        })
                )
                .map(user -> {
                    String[] ids = getAllLocationIdsAndTimezone(newShiftDto.getUserId(), newShiftDto.getLocationRoleId(),
                            user.getOrganizationId());
                    return ShiftsMapper.shiftDtoToEntity(newShiftDto, ids, user.getId(), user.getOrganizationId());
                })
                .map(newShift -> {
                    List<String> conflicts = validateShift(newShift, shiftSchedulingPreferences);
                    if (conflicts.size() > 0) {
                        newShift.setConflict(true);
                        newShift.setConflictReason(conflicts.toString());
                    }
                    return newShift;
                })
                .map(shiftsRepository::save)
                .map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                        "New Shift created: {}", shift))
                .orElseThrow(() -> new EntityNotFoundException("user"));
        // TODO Send Notification to employee if shift is released.
        // TODO Send notification to admin if conflicting shifts are there.
    }

    /**
     * @param id for shift to be deleted
     */
    public void deleteShift(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> shiftsRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(shift -> {
                            if (shift.getStart().isBefore(Instant.now())) {
                                throw new PastValueNotDeletableException("Shift");
                            }
                            if (!shift.getOrganizationId().equals(user.getOrganizationId()) ||
                                    (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                            !shift.getLocationId().equals(user.getLocationId()))) {
                                throw new UnauthorizedException();
                            }
                            shift.setDeleteFlag(true);
                            return shiftsRepository.save(shift);
                        }).map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                                "Shift deleted: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                );

    }

    /**
     * @param userId                 userId to whom shift is to be assigned (can be null)
     * @param locationRoleId         location role id for which shift is created
     * @param loggedInOrganizationId organizationId for logged-in user
     * @return {@link String[]} with locationId at index 0, locationRoleId at index 1, and location timezone
     * at index 2
     */
    private String[] getAllLocationIdsAndTimezone(String userId, String locationRoleId, String loggedInOrganizationId) {
        String[] ids = new String[3];
        if (StringUtils.isNotEmpty(userId)) {
            userRepository.findOneByIdAndDeleteFlag(userId, false)
                    .map(user -> {
                        if (user.getOrganizationId().equals(loggedInOrganizationId)) return user;
                        throw new UnauthorizedException();
                    })
                    .ifPresentOrElse(user -> {
                        ids[0] = user.getLocationId();
                        ids[1] = user.getLocationRoleId();
                    }, () -> log.error("UserId ({}) not found while creating new shift", userId));
        } else {
            locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleId, false)
                    .map(locationRole -> {
                        if (locationRole.getOrganizationId().equals(loggedInOrganizationId)) return locationRole;
                        throw new UnauthorizedException();
                    })
                    .ifPresentOrElse(locationRole -> {
                        ids[0] = locationRole.getLocationId();
                        ids[1] = locationRoleId;
                    }, () -> log.error("LocationRoleId ({}) not found while creating new shift", locationRoleId));
        }
        if (StringUtils.isEmpty(ids[0])) throw new EntityNotFoundException("user / location role");
        locationRepository.findOneByIdAndDeleteFlag(ids[0], false)
                .ifPresentOrElse(location -> ids[2] = location.getTimezone(), null);
        return ids;
    }

    /**
     * @param shift                      new shift entity being created
     * @param shiftSchedulingPreferences scheduling preferences for location role
     * @return List of any conflicts between shift scheduling and preferences
     */
    private List<String> validateShift(Shifts shift, ShiftSchedulingPreferences shiftSchedulingPreferences) {
        // Fetch shifts for max consecutive allowed days in past and future of shift dates
        Instant[] dateRangeForConsecutiveCheck = DateAndTimeUtils.getStartAndEndTimeForInstant(shift.getStart()
                        .minus(shiftSchedulingPreferences.getMaxConsecutiveWorkDays(), ChronoUnit.DAYS),
                shiftSchedulingPreferences.getMaxConsecutiveWorkDays() * 2);
        List<Shifts> shiftsToCheck = shiftsRepository.findAllByUserIdAndStartBetween(shift.getUserId(),
                dateRangeForConsecutiveCheck[0], dateRangeForConsecutiveCheck[1]);
        return ShiftSchedulingUtils.validateShift(shift, shiftSchedulingPreferences, shiftsToCheck);
    }

}