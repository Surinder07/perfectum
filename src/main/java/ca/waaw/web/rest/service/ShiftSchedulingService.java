package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.domain.*;
import ca.waaw.domain.joined.BatchDetails;
import ca.waaw.domain.joined.EmployeePreferencesWithUser;
import ca.waaw.domain.joined.ShiftDetails;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.ShiftBatchStatus;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.repository.*;
import ca.waaw.repository.joined.BatchDetailsRepository;
import ca.waaw.repository.joined.ShiftDetailsRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.CachingService;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.errors.exceptions.application.ShiftOverlappingException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.ShiftSchedulingUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ShiftSchedulingService {

    private final Logger log = LogManager.getLogger(ShiftSchedulingService.class);

    private final ShiftsRepository shiftsRepository;

    private final DetailedShiftRepository detailedShiftRepository;

    private final ShiftsBatchRepository shiftsBatchRepository;

    private final ShiftBatchMappedUserAndRoleRepository shiftBatchMappedUserAndRoleRepository;

    private final ShiftDetailsRepository shiftDetailsRepository;

    private final BatchDetailsRepository batchDetailsRepository;

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final OrganizationHolidayRepository holidayRepository;

    private final EmployeePreferencesWithUserRepository employeePreferencesWithUserRepository;

    private final CachingService cachingService;

    private final AppCustomIdConfig appCustomIdConfig;

    /**
     * Create a new Shift
     *
     * @param newShiftDto New shift details
     */
    public ApiResponseMessageDto createShift(NewShiftDto newShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        String timezone = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                        user.getOrganization().getTimezone() : user.getLocation().getTimezone())
                .orElse(null);
        ShiftsBatch batch = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> {
                    if (admin.getAuthority().equals(Authority.MANAGER))
                        newShiftDto.setLocationId(admin.getLocationId());
                    if (StringUtils.isNotEmpty(newShiftDto.getLocationId())) {
                        return locationRepository.findOneByIdAndDeleteFlag(newShiftDto.getLocationId(), false)
                                .map(location -> {
                                    if (!location.getOrganizationId().equals(admin.getOrganizationId())) {
                                        return null;
                                    }
                                    return admin;
                                })
                                .orElseThrow(() -> new EntityNotFoundException("location"));
                    }
                    return admin;
                })
                .map(admin -> {
                    ShiftsBatch newBatch = ShiftsMapper.dtoToEntityBatch(newShiftDto, admin.getOrganization().getTimezone());
                    newBatch.setWaawId(getNewBatchId(admin.getOrganization()));
                    newBatch.setCreatedBy(admin.getId());
                    newBatch.setOrganizationId(admin.getOrganizationId());
                    shiftsBatchRepository.save(newBatch);
                    if (newBatch.getMappedUsersAndRoles() != null && newBatch.getMappedUsersAndRoles().size() > 0) {
                        shiftBatchMappedUserAndRoleRepository.saveAll(newBatch.getMappedUsersAndRoles());
                    }
                    return newBatch;
                })
                .orElseThrow(AuthenticationException::new);
        CompletableFuture.runAsync(() -> {
            try {
                createShifts(newShiftDto, batch, timezone);
                batch.setStatus(batch.isReleased() ? ShiftBatchStatus.RELEASED : ShiftBatchStatus.CREATED);
            } catch (Exception e) {
                log.error("Exception while creating shifts", e);
                batch.setStatus(ShiftBatchStatus.FAILED);
            }
            shiftsBatchRepository.save(batch);
        });
        return new ApiResponseMessageDto("Shifts creation in progress. You will be notified once they are finished");
    }

    private void createShifts(NewShiftDto shiftDto, ShiftsBatch batch, String timezone) {
        List<Notification> notifications = new ArrayList<>();
        if (shiftDto.getType().equals("SINGLE")) {
            if (shiftDto.getUserIds() != null && shiftDto.getUserIds().size() > 0) {
                List<ShiftSchedulingPreferences> shiftSchedulingPreferences = getAllPreferencesForALocationOrUser(null,
                        null, shiftDto.getUserIds());
                List<Shifts> shifts = shiftDto.getUserIds().stream()
                        .map(userId -> {
                            Shifts shift = ShiftsMapper.shiftDtoToEntity(shiftDto, userId, null, null,
                                    batch.getCreatedBy(), batch.getOrganizationId(), timezone, batch.getId());
                            if (isShiftOverlapping(shift, userId)) {
                                shift.setShiftStatus(ShiftStatus.FAILED);
                                shift.setNotes("An existing shift overlaps with this shift.");
                            }
                            List<String> conflicts = validateShift(shift, Objects.requireNonNull(shiftSchedulingPreferences
                                    .stream().filter(pref -> pref.getUserId().equals(userId)).findFirst().orElse(null)));
                            return shift;
                        })
                        .collect(Collectors.toList());
                shiftsRepository.saveAll(shifts);
            } else if (shiftDto.getLocationRoleIds() != null && shiftDto.getLocationRoleIds().size() > 0) {
                List<Shifts> shifts = shiftDto.getLocationRoleIds().stream()
                        .map(roleId -> ShiftsMapper.shiftDtoToEntity(shiftDto, null, shiftDto.getLocationId(), roleId,
                                batch.getCreatedBy(), batch.getOrganizationId(), timezone, batch.getId()))
                        .collect(Collectors.toList());
                shiftsRepository.saveAll(shifts);
            } else {
                Shifts shift = ShiftsMapper.shiftDtoToEntity(shiftDto, null, shiftDto.getLocationId(), null,
                        batch.getCreatedBy(), batch.getOrganizationId(), timezone, batch.getId());
                shiftsRepository.save(shift);
            }
        } else {
            createShiftsForBatch(shiftDto, batch, timezone);
        }
        // TODO Send Notification to employee if shift is released.
        // TODO Send Notification to admin if conflicting shifts are there.
    }

    /**
     * @param id for shift to be deleted
     */
    public void deleteShift(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
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
                            shift.setLastModifiedBy(user.getId());
                            return shiftsRepository.save(shift);
                        }).map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                                "Shift deleted: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                );

    }

    /**
     * @param id for shift to be released
     */
    public void releaseShift(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
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
//                            shift.setShiftStatus(ShiftStatus.SCHEDULED);
                                    shift.setLastModifiedBy(user.getId());
                                    return shiftsRepository.save(shift);
                                }).map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                                        "Shift released: {}", id))
                                .orElseThrow(() -> new EntityNotFoundException("shift"))
                );
        // TODO send notification to employee.
    }

    /**
     * @param id for shift to be released
     */
    public void assignShift(String id, String userId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> shiftsRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(shift -> {
                            if (shift.getStart().isBefore(Instant.now())) {
                                throw new PastValueNotDeletableException("Shift");
                            }
                            if (!shift.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                            !shift.getLocationId().equals(admin.getLocationId()))) {
                                throw new UnauthorizedException();
                            }
                            return userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                                    .map(user -> {
                                        if (!user.getLocationRoleId().equals(shift.getLocationRoleId())) {
                                            throw new UnauthorizedException();
                                        }
                                        if (isShiftOverlapping(shift, userId)) {
                                            throw new ShiftOverlappingException();
                                        }
                                        shift.setUserId(user.getId());
                                        shift.setLastModifiedBy(admin.getId());
                                        return shift;
                                    })
                                    .orElseThrow(() -> new EntityNotFoundException("user"));
                        }).map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                                "Shift released: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                );
        // TODO send notification to user.
    }

    public PaginationDto getAllShifts(int pageNo, int pageSize, String searchKey, String locationId, String roleId,
                                      String startDate, String endDate, String batchStatus, String shiftStatus) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElse(null);
        assert admin != null;
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                admin.getOrganization().getTimezone() : admin.getLocation().getTimezone();
        Instant[] startEnd = StringUtils.isEmpty(startDate) && StringUtils.isNotEmpty(endDate) ?
                DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone) : new Instant[]{null, null};
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        //TODO get Status for batch and shift from string
        Page<BatchDetails> batchPage = admin.getAuthority().equals(Authority.ADMIN) ?
                batchDetailsRepository.searchAndFilterShifts(searchKey, admin.getOrganizationId(), null, startEnd[0], startEnd[1],
                        null, getSortedByCreatedDate) : batchDetailsRepository.searchAndFilterShifts(searchKey,
                null, admin.getLocationId(), startEnd[0], startEnd[1], null, getSortedByCreatedDate);
        List<String> batchIds = batchPage.getContent().stream().map(BatchDetails::getId).collect(Collectors.toList());
        Map<String, List<ShiftDetails>> shifts = shiftDetailsRepository.searchAndFilterShifts(searchKey, locationId, roleId, null,
                        null, SecurityUtils.isCurrentUserInRole(Authority.ADMIN), batchIds)
                .stream().collect(Collectors.groupingBy(ShiftDetails::getBatchId));
        return CommonUtils.getPaginationResponse(batchPage, ShiftsMapper::entitiesToListingDto, shifts, timezone);
    }

    public PaginationDto getAllShiftsUser(int pageNo, int pageSize, String userId, String startDate, String endDate, String shiftStatus) {
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElse(null);
        assert admin != null;
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                admin.getOrganization().getTimezone() : admin.getLocation().getTimezone();
        Instant[] startEnd = StringUtils.isEmpty(startDate) && StringUtils.isNotEmpty(endDate) ?
                DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone) : new Instant[]{null, null};
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        if (StringUtils.isNotEmpty(userId)) {
            userRepository.findOneByIdAndDeleteFlag(userId, false)
                    .map(user -> {
                        if ((!user.getOrganizationId().equals(admin.getOrganizationId())) ||
                                admin.getAuthority().equals(Authority.MANAGER) &&
                                        !user.getLocationId().equals(admin.getLocationId())) {
                            return null;
                        }
                        return user;
                    })
                    .orElseThrow(() -> new EntityNotFoundException("user"));
        } else if (!admin.getAuthority().equals(Authority.ADMIN)) {
            userId = admin.getId();
        }
        Page<ShiftDetails> shiftsPage = shiftDetailsRepository.searchAndFilterShiftsDate(null, null,
                null, null, userId, true, startEnd[0], startEnd[1], getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(shiftsPage, ShiftsMapper::entityToShiftDto, timezone);
    }

    /**
     * @param newShiftDto new shift batch details
     * @param batch       batch details
     */
    @Transactional(rollbackFor = Exception.class)
    private void createShiftsForBatch(NewShiftDto newShiftDto, ShiftsBatch batch, String timezone) {
        log.info("Starting shift creation asynchronously");
        // Fetch all preferences for given location
        List<ShiftSchedulingPreferences> preferences = getAllPreferencesForALocationOrUser(newShiftDto.getLocationId(),
                newShiftDto.getLocationRoleIds(), newShiftDto.getUserIds());
        // Fetch all shifts for given location in given range (plus minus 1 week each side)
        List<Shifts> existingShifts = shiftsRepository.findAllByLocationIdAndStartBetween(batch.getLocationId(),
                batch.getStartDate().minus(7, ChronoUnit.DAYS), batch.getEndDate().plus(7, ChronoUnit.DAYS));
        // Fetch all holidays for given location in given range
        // TODO validate and accommodate for next year holidays if needed
        List<OrganizationHolidays> holidays = holidayRepository.getAllForLocationAndMonthIfNeeded(batch.getLocationId(),
                null, DateAndTimeUtils.getCurrentDate("year", timezone));
        // TODO Fetch all timeOffs for given location in given range
        // Fetch all employee preference
        List<EmployeePreferencesWithUser> employeePreferenceWithUsers = getEmployeesAndPreference(newShiftDto.getLocationId(),
                newShiftDto.getLocationRoleIds(), newShiftDto.getUserIds(), batch.getOrganizationId());
        // Validate and create shifts for the batch
        List<String> employeeWithoutPreferences = new ArrayList<>();
        List<Shifts> newShifts = ShiftSchedulingUtils.validateAndCreateShiftsForBatch(batch, existingShifts,
                holidays, preferences, employeePreferenceWithUsers, timezone, employeeWithoutPreferences);
        if (newShifts.size() > 0) {
            shiftsRepository.saveAll(newShifts);
            // TODO use employeeWithoutPreferences to send notification to admin
            // TODO send notification to admin for gaps if any
        } else {
            log.info("No new shifts wew created for batch: {}", batch.getName());
        }
    }

    /**
     * @param locationId      locationId
     * @param locationRoleIds list of locationRoleIds
     * @param userIds         list of user ids
     * @return Employee preference based on one of these ids, hierarchy followed -> userid, locationRoleId, locationId
     */
    private List<EmployeePreferencesWithUser> getEmployeesAndPreference(String locationId, List<String> locationRoleIds,
                                                                        List<String> userIds, String organizationId) {
        if (userIds != null && userIds.size() > 0) {
            return employeePreferencesWithUserRepository.findAllByIsExpiredAndDeleteFlagAndUserIdIn(false, false, userIds);
        } else if (locationRoleIds != null && locationRoleIds.size() > 0) {
            return employeePreferencesWithUserRepository.findAllByLocationRoleIdInAndIsExpiredAndDeleteFlag(locationRoleIds, false, false);
        } else if (StringUtils.isNotEmpty(locationId)) {
            return employeePreferencesWithUserRepository.findAllByLocationIdAndIsExpiredAndDeleteFlag(locationId, false, false);
        } else
            return employeePreferencesWithUserRepository.findAllByOrganizationIdAndIsExpiredAndDeleteFlag(organizationId, false, false);
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
     * @param locationId      locationId
     * @param locationRoleIds locationRoleIds
     * @param userIds         userIds
     * @return List of shift scheduling preference based on one of these ids, hierarchy followed ->
     * userId, locationRoleId, locationId
     */
    private List<ShiftSchedulingPreferences> getAllPreferencesForALocationOrUser(String locationId, List<String> locationRoleIds,
                                                                                 List<String> userIds) {
        if (userIds != null && userIds.size() > 0) {
            return userOrganizationRepository.findAllByDeleteFlagAndIdIn(false, userIds)
                    .stream()
                    .map(user -> ShiftSchedulingUtils.mappingFunction(user.getLocationRole(), user.getId()))
                    .collect(Collectors.toList());
        } else if (locationRoleIds != null && locationRoleIds.size() > 0) {
            return locationRoleRepository.findAllByDeleteFlagAndIdIn(false, locationRoleIds)
                    .stream().map(ShiftSchedulingUtils::mappingFunction)
                    .collect(Collectors.toList());
        } else {
            return locationRoleRepository.findAllByLocationIdAndDeleteFlag(locationId, false)
                    .stream().map(ShiftSchedulingUtils::mappingFunction).collect(Collectors.toList());
        }
    }

    /**
     * @param batch batch object
     * @return shifts associated with the batch
     */
    private List<Shifts> getShiftsFromBatch(ShiftsBatch batch) {
        List<Shifts> shifts;
//        if (batch.getUsers() != null && batch.getUsers().size() > 0) {
//            shifts = shiftsRepository.findAllByUserIdInAndStartBetween(batch.getUsers(), batch.getStartDate(), batch.getEndDate());
//        } else if (StringUtils.isNotEmpty(batch.getLocationRoleId())) {
//            shifts = shiftsRepository.findAllByLocationRoleIdAndStartBetween(batch.getLocationRoleId(), batch.getStartDate(),
//                    batch.getEndDate());
//        } else {
//            shifts = shiftsRepository.findAllByLocationIdAndStartBetween(batch.getLocationId(), batch.getStartDate(),
//                    batch.getEndDate());
//        }
//        return shifts.stream().filter(shift -> shift.getShiftType().equals(ShiftType.RECURRING)).collect(Collectors.toList());
        return null;
    }

    /**
     * @param batch batch object
     * @return shifts associated with the batch
     */
    private List<ShiftDetails> getDetailedShiftsFromBatch(ShiftsBatch batch) {
        List<ShiftDetails> shifts;
//        if (batch.getUsers() != null && batch.getUsers().size() > 0) {
//            shifts = detailedShiftRepository.findAllByUser_idInAndDeleteFlagAndStartBetween(batch.getUsers(),
//                    false, batch.getStartDate(), batch.getEndDate());
//        } else if (StringUtils.isNotEmpty(batch.getLocationRoleId())) {
//            shifts = detailedShiftRepository.findAllByLocationRole_idAndDeleteFlagAndStartBetween(batch.getLocationRoleId(),
//                    false, batch.getStartDate(), batch.getEndDate());
//        } else {
//            shifts = detailedShiftRepository.findAllByLocation_idAndDeleteFlagAndStartBetween(batch.getLocationId(),
//                    false, batch.getStartDate(), batch.getEndDate());
//        }
//        return shifts.stream().filter(shift -> shift.getShiftType().equals(ShiftType.RECURRING)).collect(Collectors.toList());
        return null;
    }

    /**
     * @param shift  shift details to be checked
     * @param userId user for whom overlapping is to be checked
     * @return true is there is shift overlapping
     */
    private boolean isShiftOverlapping(Shifts shift, String userId) {
        return !shiftsRepository.findAllByUserIdAndStartBetween(userId, shift.getStart(), shift.getEnd())
                .isEmpty();
    }

    private String getNewBatchId(Organization organization) {
        int orgPrefix = cachingService.getOrganizationPrefix(organization.getId(), organization.getName());
        String lastName = shiftsBatchRepository.getLastUsedId(organization.getId())
                .orElse("xxx0000000000");
        String newNumber = String.valueOf(Integer.parseInt(lastName.substring(4)) + 1);
        String nameSuffix = StringUtils.leftPad(newNumber, appCustomIdConfig.getLength()
                - newNumber.length(), '0');
        return organization.getName().substring(0, 3) + orgPrefix + nameSuffix;
    }

}