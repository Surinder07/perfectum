package ca.waaw.web.rest.service;

import ca.waaw.domain.*;
import ca.waaw.domain.joined.EmployeePreferencesWithUser;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.dto.shifts.NewShiftBatchDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.repository.*;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.utils.ApiResponseMessageKeys;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.ShiftSchedulingUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ShiftSchedulingService {

    private final Logger log = LogManager.getLogger(ShiftSchedulingService.class);

    private final ShiftsRepository shiftsRepository;

    private final ShiftsBatchRepository shiftsBatchRepository;

    private final ShiftBatchMappedUserRepository shiftBatchMappedUserRepository;

    private final UserOrganizationRepository userRepository;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final OrganizationHolidayRepository holidayRepository;

    private final EmployeePreferencesWithUserRepository employeePreferencesWithUserRepository;

    /**
     * Create a new Shift
     *
     * @param newShiftDto New shift details
     */
    public void createShift(NewShiftDto newShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    String[] ids = getAllLocationIdsAndTimezone(newShiftDto.getUserId(), newShiftDto.getLocationRoleId(),
                            user.getOrganizationId());
                    return ShiftsMapper.shiftDtoToEntity(newShiftDto, ids, user.getId(), user.getOrganizationId());
                })
                .map(newShift -> {
                    ShiftSchedulingPreferences shiftSchedulingPreferences = getAllPreferencesForALocationOrUser(null,
                            newShift.getLocationRoleId(), null).get(0);
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
     * @param newShiftBatchDto new shift batch details
     * @return Response message
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponseMessageDto createNewBatch(NewShiftBatchDto newShiftBatchDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    if (user.getAuthority().equals(Authority.MANAGER))
                        newShiftBatchDto.setLocationId(user.getLocationId());
                    else locationRepository.findOneByIdAndDeleteFlag(newShiftBatchDto.getLocationId(), false)
                            .ifPresent(location -> {
                                if (!location.getOrganizationId().equals(user.getOrganizationId())) {
                                    throw new UnauthorizedException();
                                }
                            });
                    return user;
                })
                .orElseThrow(UnauthorizedException::new);
        String timezone = locationRepository.findOneByIdAndDeleteFlag(newShiftBatchDto.getLocationId(), false)
                .map(Location::getTimezone)
                .orElse(null);
        ShiftsBatch batch = ShiftsMapper.dtoToEntityBatch(newShiftBatchDto, timezone);
        batch.setCreatedBy(admin.getId());
        batch.setOrganizationId(admin.getOrganizationId());
        shiftsBatchRepository.save(batch);
        if (batch.getMappedUsers() != null && batch.getMappedUsers().size() > 0) {
            shiftBatchMappedUserRepository.saveAll(batch.getMappedUsers());
        }

        CompletableFuture.runAsync(() -> {
            log.info("Starting shift creation asynchronously");
            // Fetch all preferences for given location
            List<ShiftSchedulingPreferences> preferences = getAllPreferencesForALocationOrUser(batch.getLocationId(),
                    batch.getLocationRoleId(), batch.getUsers());
            // Fetch all shifts for given location in given range (plus minus 1 week each side)
            List<Shifts> existingShifts = shiftsRepository.findAllByLocationIdAndStartBetween(batch.getLocationId(),
                    batch.getStartDate().minus(7, ChronoUnit.DAYS), batch.getEndDate().plus(7, ChronoUnit.DAYS));
            // Fetch all holidays for given location in given range
            // TODO validate and accommodate for next year holidays if needed
            List<OrganizationHolidays> holidays = holidayRepository.getAllForLocationAndMonthIfNeeded(batch.getLocationId(),
                    null, DateAndTimeUtils.getCurrentDate("year", timezone));
            // TODO Fetch all timeOffs for given location in given range
            // Fetch all employee preference
            List<EmployeePreferencesWithUser> employeePreferenceWithUsers = getEmployeesAndPreference(batch.getLocationId(),
                    batch.getLocationRoleId(), batch.getUsers());
            // Validate and create shifts for the batch
            List<String> employeeWithoutPreferences = new ArrayList<>();
            List<Shifts> newShifts = ShiftSchedulingUtils.validateAndCreateShiftsForBatch(batch, existingShifts,
                    holidays, preferences, employeePreferenceWithUsers, timezone, admin.getId(), employeeWithoutPreferences);
            shiftsRepository.saveAll(newShifts);
            // TODO use employeeWithoutPreferences to send notification to admin
        });
        return new ApiResponseMessageDto(CommonUtils.getPropertyFromMessagesResourceBundle(ApiResponseMessageKeys
                .createNewBatch, new Locale(admin.getLangKey())));
    }

    /**
     * @param locationId     locationId
     * @param locationRoleId locationRoleId
     * @param userIds        list of user ids
     * @return Employee preference based on one of these ids, hierarchy followed -> userid, locationRoleId, locationId
     */
    private List<EmployeePreferencesWithUser> getEmployeesAndPreference(String locationId, String locationRoleId,
                                                                        List<String> userIds) {
        if (userIds != null && userIds.size() > 0) {
            return employeePreferencesWithUserRepository.findAllByIsExpiredAndDeleteFlagAndUserIdIn(false, false, userIds);
        } else if (StringUtils.isNotEmpty(locationRoleId)) {
            return employeePreferencesWithUserRepository.findAllByLocationRoleIdAndIsExpiredAndDeleteFlag(locationRoleId, false, false);
        } else {
            return employeePreferencesWithUserRepository.findAllByLocationIdAndIsExpiredAndDeleteFlag(locationId, false, false);
        }
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

    /**
     * @param locationId     location for which all preferences are required
     * @param locationRoleId locationRoleId
     * @param userIds        userIds
     * @return List of shift scheduling preference based on one of these ids,
     * hierarchy followed -> userid, locationRoleId, locationId
     */
    private List<ShiftSchedulingPreferences> getAllPreferencesForALocationOrUser(String locationId, String locationRoleId,
                                                                                 List<String> userIds) {
        return getAllPreferencesForALocationOrUser(locationId, locationRoleId, userIds, ShiftSchedulingUtils::mappingFunction);
    }

    /**
     * @param locationId     locationId
     * @param locationRoleId locationRoleId
     * @param userIds        userIds
     * @param function       function to map location role to shift scheduling preferences
     * @return List of shift scheduling preference based on one of these ids,
     * hierarchy followed -> userid, locationRoleId, locationId
     */
    private List<ShiftSchedulingPreferences> getAllPreferencesForALocationOrUser(String locationId, String locationRoleId, List<String> userIds,
                                                                                 Function<LocationRole, ShiftSchedulingPreferences> function) {
        if (userIds != null && userIds.size() > 0) {
            return userRepository.findAllByDeleteFlagAndIdIn(false, userIds)
                    .stream().map(UserOrganization::getLocationRole).map(function)
                    .collect(Collectors.toList());
        } else if (StringUtils.isNotEmpty(locationRoleId)) {
            return locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleId, false)
                    .map(function).map(Collections::singletonList).orElse(null);
        } else {
            return locationRoleRepository.findAllByLocationIdAndDeleteFlag(locationId, false)
                    .stream().map(function).collect(Collectors.toList());
        }
    }

}