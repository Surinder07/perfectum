package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.domain.*;
import ca.waaw.domain.joined.*;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.shifts.UpdateShiftDto;
import ca.waaw.enumration.*;
import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.repository.*;
import ca.waaw.repository.joined.*;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.CachingService;
import ca.waaw.service.WebSocketService;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ShiftSchedulingService {

    private final Logger log = LogManager.getLogger(ShiftSchedulingService.class);

    private final ShiftsRepository shiftsRepository;

    private final ShiftsBatchRepository shiftsBatchRepository;

    private final ShiftBatchMappedUserAndRoleRepository shiftBatchMappedUserAndRoleRepository;

    private final ShiftDetailsRepository shiftDetailsRepository;

    private final ShiftDetailsWithBatchRepository shiftDetailsWithBatchRepository;

    private final BatchDetailsRepository batchDetailsRepository;

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final OrganizationHolidayRepository holidayRepository;

    private final EmployeePreferencesWithUserRepository employeePreferencesWithUserRepository;

    private final RequestsRepository requestsRepository;

    private final CachingService cachingService;

    private final WebSocketService webSocketService;

    private final AppCustomIdConfig appCustomIdConfig;

    /**
     * Create a new Shift
     *
     * @param newShiftDto New shift details
     */
    public ApiResponseMessageDto createShift(NewShiftDto newShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        AtomicReference<String> loggedUsername = new AtomicReference<>();
        AtomicReference<String> timezone = new AtomicReference<>();
        ShiftsBatch batch = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> {
                    loggedUsername.set(admin.getUsername());
                    timezone.set(admin.getAuthority().equals(Authority.ADMIN) ?
                            admin.getOrganization().getTimezone() : admin.getLocation().getTimezone());
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
                createShifts(newShiftDto, batch, timezone.get());
            } catch (Exception e) {
                log.error("Exception while creating shifts", e);
                batch.setStatus(ShiftBatchStatus.FAILED);
                shiftsBatchRepository.save(batch);
            }
            webSocketService.notifyUserAboutShiftCreation(loggedUsername.get());
        });
        return new ApiResponseMessageDto("Shifts creation in progress. You will be notified once they are finished");
    }

    private void createShifts(NewShiftDto shiftDto, ShiftsBatch batch, String timezone) {
        List<Notification> notifications = new ArrayList<>();
        AtomicReference<String> currentCustomId = new AtomicReference<>(shiftsRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000"));
        boolean batchFail = false;
        if (shiftDto.getType().equals("SINGLE")) {
            if (shiftDto.getUserIds() != null && shiftDto.getUserIds().size() > 0) {
                List<ShiftSchedulingPreferences> shiftSchedulingPreferences = getAllPreferencesForALocationOrUser(null,
                        null, shiftDto.getUserIds());
                List<User> users = userRepository.findAllByIdInAndDeleteFlag(shiftDto.getUserIds(), false);
                List<Shifts> shifts = shiftDto.getUserIds().stream()
                        .map(userId -> {
                            String customId = CommonUtils.getNextCustomId(currentCustomId.get(), appCustomIdConfig.getLength());
                            Shifts shift = ShiftsMapper.shiftDtoToEntity(shiftDto, userId, null, null,
                                    batch.getCreatedBy(), batch.getOrganizationId(), timezone, batch.getId(), customId);
                            currentCustomId.set(customId);
                            User user = users.stream().filter(user1 -> user1.getId().equals(userId))
                                    .findFirst().orElse(null);
                            assert user != null;
                            shift.setLocationId(user.getLocationId());
                            shift.setLocationRoleId(user.getLocationRoleId());
                            int sameDayTimeOff = (int) requestsRepository.getOverlappingForDates(shift.getStart(), shift.getEnd(), false)
                                    .stream().filter(request -> request.getUserId().equals(shift.getUserId()))
                                    .filter(requests -> requests.getType().equals(RequestType.TIME_OFF))
                                    .filter(requests -> requests.getStatus().equals(RequestStatus.ACCEPTED))
                                    .count();
                            if (sameDayTimeOff > 0) {
                                shift.setShiftStatus(ShiftStatus.FAILED);
                                shift.setNotes("A time off request is already approved for this time.");
                            }
                            List<String> conflicts = validateShift(shift, Objects.requireNonNull(shiftSchedulingPreferences
                                    .stream().filter(pref -> pref.getUserId().equals(userId)).findFirst().orElse(null)));
                            return shift;
                        })
                        .collect(Collectors.toList());
                shiftsRepository.saveAll(shifts);
                batchFail = shifts.stream().allMatch(shift -> shift.getShiftStatus().equals(ShiftStatus.FAILED));
            } else if (shiftDto.getLocationRoleIds() != null && shiftDto.getLocationRoleIds().size() > 0) {
                List<Shifts> shifts = shiftDto.getLocationRoleIds().stream()
                        .map(roleId -> {
                            String customId = CommonUtils.getNextCustomId(currentCustomId.get(), appCustomIdConfig.getLength());
                            currentCustomId.set(customId);
                            return ShiftsMapper.shiftDtoToEntity(shiftDto, null, shiftDto.getLocationId(), roleId,
                                    batch.getCreatedBy(), batch.getOrganizationId(), timezone, batch.getId(), customId);
                        })
                        .collect(Collectors.toList());
                shiftsRepository.saveAll(shifts);
            } else {
                String customId = CommonUtils.getNextCustomId(currentCustomId.get(), appCustomIdConfig.getLength());
                currentCustomId.set(customId);
                Shifts shift = ShiftsMapper.shiftDtoToEntity(shiftDto, null, shiftDto.getLocationId(), null,
                        batch.getCreatedBy(), batch.getOrganizationId(), timezone, batch.getId(), customId);
                shiftsRepository.save(shift);
            }
            if (batchFail) batch.setStatus(ShiftBatchStatus.FAILED);
        } else {
            createShiftsForBatch(shiftDto, batch, timezone, currentCustomId, shiftDto.isInstantRelease());
        }
        if (!batch.getStatus().equals(ShiftBatchStatus.FAILED))
            batch.setStatus(shiftDto.isInstantRelease() ? ShiftBatchStatus.RELEASED : ShiftBatchStatus.CREATED);
        shiftsBatchRepository.save(batch);
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
                                return null;
                            }
                            if (user.getAuthority().equals(Authority.MANAGER)) {
                                return locationRoleRepository.findOneByIdAndDeleteFlag(shift.getLocationRoleId(), false)
                                        .map(role -> {
                                            if (role.isAdminRights()) return null;
                                            else {
                                                shift.setDeleteFlag(true);
                                                shift.setLastModifiedBy(user.getId());
                                                return shiftsRepository.save(shift);
                                            }
                                        });
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
     * @param id for batch to be deleted
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> shiftsBatchRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(batch -> {
                            if (batch.getStartDate().isBefore(Instant.now())) {
                                throw new PastValueNotDeletableException("Shift");
                            }
                            if (!batch.getOrganizationId().equals(user.getOrganizationId()) ||
                                    (user.getAuthority().equals(Authority.MANAGER) &&
                                            !batch.getLocationId().equals(user.getLocationId()))) {
                                return null;
                            }
                            batch.setDeleteFlag(true);
                            batch.setLastModifiedBy(user.getId());
                            return shiftsBatchRepository.save(batch);
                        })
                        .map(batch -> {
                            List<Shifts> shiftsToDelete = shiftsRepository.findAllByBatchIdAndDeleteFlag(batch.getId(), false)
                                    .stream().peek(shift -> {
                                        shift.setDeleteFlag(true);
                                        shift.setLastModifiedBy(user.getId());
                                    }).collect(Collectors.toList());
                            shiftsRepository.saveAll(shiftsToDelete);
                            return batch;
                        })
                        .map(batch -> CommonUtils.logMessageAndReturnObject(batch, "info", ShiftSchedulingService.class,
                                "Batch deleted: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("batch"))
                );

    }

    /**
     * @param id for shift to be released
     */
    @Transactional(rollbackFor = Exception.class)
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
                                return null;
                            }
                            shift.setShiftStatus(ShiftStatus.RELEASED);
                            shift.setLastModifiedBy(user.getId());
                            return shiftsRepository.save(shift);
                        }).map(shift -> {
                            long unReleasedShiftInBatch = shiftsRepository
                                    .findAllByBatchIdAndDeleteFlag(shift.getBatchId(), false)
                                    .stream()
                                    .filter(shift1 -> !shift1.getShiftStatus().equals(ShiftStatus.RELEASED))
                                    .count();
                            if (unReleasedShiftInBatch == 0) {
                                shiftsBatchRepository.findOneByIdAndDeleteFlag(shift.getBatchId(), false)
                                        .map(batch -> {
                                            batch.setReleased(true);
                                            batch.setStatus(ShiftBatchStatus.RELEASED);
                                            batch.setLastModifiedBy(shift.getLastModifiedBy());
                                            return batch;
                                        })
                                        .map(shiftsBatchRepository::save);
                            }
                            return shift;
                        })
                        .map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                                "Shift released: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                );
        // TODO send notification to employee.
    }

    /**
     * @param id id for batch to be released
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseBatch(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> shiftsBatchRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(batch -> {
                            if (!batch.getOrganizationId().equals(user.getOrganizationId()) ||
                                    (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                            !batch.getLocationId().equals(user.getLocationId()))) {
                                return null;
                            }
                            batch.setReleased(true);
                            batch.setStatus(ShiftBatchStatus.RELEASED);
                            batch.setLastModifiedBy(user.getId());
                            return batch;
                        })
                        .map(batch -> {
                            List<Shifts> shiftsToRelease = shiftsRepository.findAllByBatchIdAndDeleteFlag(batch.getId(), false)
                                    .stream().peek(shift -> {
                                        if (shift.getStart().isBefore(Instant.now())) {
                                            shift.setShiftStatus(ShiftStatus.FAILED);
                                            shift.setNotes("Couldn't release shift as it is in past.");
                                        } else {
                                            shift.setShiftStatus(ShiftStatus.RELEASED);
                                        }
                                        shift.setLastModifiedBy(user.getId());
                                    }).collect(Collectors.toList());
                            shiftsRepository.saveAll(shiftsToRelease);
                            if (shiftsToRelease.stream().anyMatch(shift -> !shift.getShiftStatus().equals(ShiftStatus.RELEASED))){
                                batch.setStatus(ShiftBatchStatus.FAILED);
                            }
                            return batch;
                        })
                        .map(shiftsBatchRepository::save)
                        .map(batch -> CommonUtils.logMessageAndReturnObject(batch, "info", ShiftSchedulingService.class,
                                "Batch released: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("batch"))
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

    public void updateShift(UpdateShiftDto updateShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> shiftsRepository.findOneByIdAndDeleteFlag(updateShiftDto.getId(), false)
                        .flatMap(shift -> userOrganizationRepository.findOneByIdAndDeleteFlag(shift.getUserId(), false)
                                .map(user -> {
                                    if (!loggedUser.getOrganizationId().equals(user.getOrganizationId()) ||
                                            (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                                    (!loggedUser.getLocationId().equals(user.getLocationId()) ||
                                                            user.getLocationRole().isAdminRights()))) return null;
                                    String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ?
                                            loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone();
                                    Instant start = DateAndTimeUtils.getDateInstant(updateShiftDto.getStart().getDate(),
                                            updateShiftDto.getStart().getTime(), timezone);
                                    Instant end = DateAndTimeUtils.getDateInstant(updateShiftDto.getEnd().getDate(),
                                            updateShiftDto.getEnd().getTime(), timezone);
                                    List<Shifts> existing = shiftsRepository.getByUserIdBetweenDates(shift.getUserId(), start, end);
                                    if (existing.size() > 0 && existing.stream().anyMatch(existingShift -> !existingShift.equals(shift))) {
                                        throw new ShiftOverlappingException();
                                    }
                                    shift.setStart(start);
                                    shift.setEnd(end);
                                    shift.setLastModifiedBy(loggedUser.getId());
                                    shift.setNotes(updateShiftDto.getComments());
                                    return shift;
                                })
                                .map(shiftsRepository::save)
                        )
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                )
                .map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftSchedulingService.class,
                        "Shift updated successfully: {}", shift));
        // todo send notification to user
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
        Instant[] startEnd = StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate) ?
                DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone) : new Instant[]{null, null};
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        Page<BatchDetails> batchPage = admin.getAuthority().equals(Authority.ADMIN) ?
                batchDetailsRepository.searchAndFilterShifts(searchKey, admin.getOrganizationId(), null, startEnd[0], startEnd[1],
                        batchStatus, getSortedByCreatedDate) : batchDetailsRepository.searchAndFilterShifts(searchKey,
                null, admin.getLocationId(), startEnd[0], startEnd[1], batchStatus, getSortedByCreatedDate);
        List<String> batchIds = batchPage.getContent().stream().map(BatchDetails::getId).collect(Collectors.toList());
        Map<String, List<ShiftDetails>> shifts = shiftDetailsRepository.searchAndFilterShifts(searchKey, locationId, roleId, shiftStatus,
                        null, admin.getAuthority().equals(Authority.ADMIN), batchIds)
                .stream().sorted(Comparator.comparing(ShiftDetails::getStart))
                .collect(Collectors.groupingBy(ShiftDetails::getBatchId));
        return CommonUtils.getPaginationResponse(batchPage, ShiftsMapper::entitiesToListingDto, shifts, timezone);
    }

    public PaginationDto getAllShiftsUser(int pageNo, int pageSize, String userId, String startDate, String endDate,
                                          String shiftStatus, String searchKey) {
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElse(null);
        assert admin != null;
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                admin.getOrganization().getTimezone() : admin.getLocation().getTimezone();
        Instant[] startEnd = StringUtils.isNotEmpty(startDate) ?
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
            shiftStatus = "RELEASED";
        }
        Page<ShiftDetailsWithBatch> shiftsPage = shiftDetailsWithBatchRepository.searchAndFilterShiftsDate(searchKey,
                shiftStatus, userId, startEnd[0], startEnd[1], getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(shiftsPage, ShiftsMapper::entityToShiftDto, timezone);
    }

    public ShiftDetailsDto getShiftById(String id) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> shiftDetailsWithBatchRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(shift -> {
                            if ((loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                    loggedUser.getLocationId().equals(shift.getLocationId())) ||
                                    !loggedUser.getOrganizationId().equals(shift.getOrganizationId())) {
                                return null;
                            }
                            String timezone = loggedUser.getAuthority().equals(Authority.MANAGER) ?
                                    loggedUser.getLocation().getTimezone() : loggedUser.getOrganization().getTimezone();
                            return ShiftsMapper.entityToShiftDto(shift, timezone);
                        }).orElseThrow(() -> new EntityNotFoundException("shift"))
                ).orElseThrow(AuthenticationException::new);
    }

    /**
     * @param newShiftDto new shift batch details
     * @param batch       batch details
     */
    @Transactional(rollbackFor = Exception.class)
    private void createShiftsForBatch(NewShiftDto newShiftDto, ShiftsBatch batch, String timezone,
                                      AtomicReference<String> currentCustomId, boolean instantRelease) {
        log.info("Starting shift creation asynchronously");
        // Fetch all preferences for given location
        List<ShiftSchedulingPreferences> preferences = getAllPreferencesForALocationOrUser(newShiftDto.getLocationId(),
                newShiftDto.getLocationRoleIds(), newShiftDto.getUserIds());
        // Fetch all shifts for given location in given range (plus minus 1 week each side)
        List<Shifts> existingShifts = shiftsRepository.findAllByLocationIdAndStartBetween(batch.getLocationId(),
                batch.getStartDate().minus(7, ChronoUnit.DAYS), batch.getEndDate().plus(7, ChronoUnit.DAYS));
        // Fetch all holidays for given location in given range
        // TODO validate and accommodate for next year holidays if needed
        List<OrganizationHolidays> holidays = holidayRepository.getAllForLocationByYear(batch.getLocationId(),
                DateAndTimeUtils.getCurrentDate("year", timezone));
        // TODO Fetch all timeOffs for given location in given range
        List<Requests> timeOff = requestsRepository.getOverlappingForDates(batch.getStartDate(), batch.getEndDate(), false)
                .stream().filter(requests -> requests.getType().equals(RequestType.TIME_OFF))
                .filter(requests -> requests.getStatus().equals(RequestStatus.ACCEPTED))
                .collect(Collectors.toList());
        // Fetch all employee preference
        List<EmployeePreferencesWithUser> employeePreferenceWithUsers = getEmployeesAndPreference(newShiftDto.getLocationId(),
                newShiftDto.getLocationRoleIds(), newShiftDto.getUserIds(), batch.getOrganizationId());
        // Validate and create shifts for the batch
        List<String> employeeWithoutPreferences = new ArrayList<>();
        List<Shifts> newShifts = ShiftSchedulingUtils.validateAndCreateShiftsForBatch(batch, existingShifts,
                holidays, preferences, employeePreferenceWithUsers, timezone, employeeWithoutPreferences, timeOff);
        newShifts.forEach(shift -> {
            String customId = CommonUtils.getNextCustomId(currentCustomId.get(), appCustomIdConfig.getLength());
            shift.setWaawId(customId);
            currentCustomId.set(customId);
            if (instantRelease && !shift.getShiftStatus().equals(ShiftStatus.FAILED))
                shift.setShiftStatus(ShiftStatus.RELEASED);
        });
        boolean batchFail = newShifts.stream().allMatch(shift -> shift.getShiftStatus().equals(ShiftStatus.FAILED));
        if (newShifts.size() > 0) {
            shiftsRepository.saveAll(newShifts);
            if (batchFail) {
                batch.setStatus(ShiftBatchStatus.FAILED);
                shiftsBatchRepository.save(batch);
            }
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
        List<Shifts> shiftsToCheck = shiftsRepository.findAllByUserIdAndStartBetweenAndDeleteFlag(shift.getUserId(),
                dateRangeForConsecutiveCheck[0], dateRangeForConsecutiveCheck[1], false);
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
     * @param shift  shift details to be checked
     * @param userId user for whom overlapping is to be checked
     * @return true is there is shift overlapping
     */
    private boolean isShiftOverlapping(Shifts shift, String userId) {
        return !shiftsRepository.findAllByUserIdAndStartBetweenAndDeleteFlag(userId, shift.getStart(), shift.getEnd(), false)
                .isEmpty();
    }

    public String getNewBatchId(Organization organization) {
        int orgPrefix = cachingService.getOrganizationPrefix(organization.getId(), organization.getName());
        String lastName = shiftsBatchRepository.getLastUsedId(organization.getId())
                .orElse("xxx0000000000");
        String newNumber = String.valueOf(Integer.parseInt(lastName.substring(4)) + 1);
        String nameSuffix = StringUtils.leftPad(newNumber, appCustomIdConfig.getLength()
                - newNumber.length(), '0');
        return organization.getName().substring(0, 3) + orgPrefix + nameSuffix;
    }

}