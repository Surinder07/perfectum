package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.shifts.ShiftDetails;
import ca.waaw.domain.shifts.ShiftWithRole;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.user.EmployeePreferencesWithUser;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.appnotifications.MultipleNotificationDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.shifts.ShiftSchedulingPreferences;
import ca.waaw.dto.shifts.UpdateShiftDto;
import ca.waaw.dto.sqldtos.ShiftBatchSqlDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestType;
import ca.waaw.enumration.shift.ShiftBatchStatus;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.mapper.ShiftsMapper;
import ca.waaw.repository.locationandroles.LocationRoleRepository;
import ca.waaw.repository.organization.OrganizationHolidayRepository;
import ca.waaw.repository.requests.RequestsRepository;
import ca.waaw.repository.shifts.ShiftDetailsRepository;
import ca.waaw.repository.shifts.ShiftWithRoleRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.user.EmployeePreferencesWithUserRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.AppNotificationService;
import ca.waaw.service.CachingService;
import ca.waaw.service.WebSocketService;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.errors.exceptions.application.ShiftNotAssignedException;
import ca.waaw.web.rest.errors.exceptions.application.ShiftOverlappingException;
import ca.waaw.web.rest.utils.*;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ShiftsService {

    private final Logger log = LogManager.getLogger(ShiftsService.class);

    private final ShiftsRepository shiftsRepository;

    private final ShiftWithRoleRepository shiftWithRoleRepository;

    private final ShiftDetailsRepository shiftDetailsRepository;

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final OrganizationHolidayRepository holidayRepository;

    private final EmployeePreferencesWithUserRepository employeePreferencesWithUserRepository;

    private final RequestsRepository requestsRepository;

    private final CachingService cachingService;

    private final WebSocketService webSocketService;

    private final AppNotificationService appNotificationService;

    private final AppCustomIdConfig appCustomIdConfig;

    private final MessageSource messageSource;

    /**
     * Create a new Shift
     *
     * @param newShiftDto New shift details
     */
    public ApiResponseMessageDto createShift(NewShiftDto newShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        CompletableFuture.runAsync(() -> {
            String newWaawBatchId = getNewBatchId(loggedUser.getOrganization());
            String newBatchId = UUID.randomUUID().toString();
            List<MultipleNotificationDto> notifications = new ArrayList<>();
            log.info("Starting creating shifts for request {}", newShiftDto);
            try {
                if (newShiftDto.getType().equalsIgnoreCase("single")) {
                    createSingleShifts(newShiftDto, newBatchId, newWaawBatchId, loggedUser, notifications);
                } else {
                    createBatchShifts(newShiftDto, newBatchId, newWaawBatchId, loggedUser, notifications);
                }
                ShiftSchedulingUtils.addNewNotificationForShift(notifications, null, null, loggedUser,
                        "SHIFT_CREATED", newWaawBatchId, null);
                log.info("Finished creating a new shift batch {}", newWaawBatchId);
                appNotificationService.sendMultipleApplicationNotification(notifications);
            } catch (Exception e) {
                log.error("ERROR while creating shifts for dto {}", newShiftDto, e);
            }
            webSocketService.notifyUserAboutShiftCreation(loggedUser.getUsername());
        });
        return new ApiResponseMessageDto(messageSource.getMessage(ApiResponseMessageKeys.creatingShiftsMessage, null, new Locale(loggedUser.getLangKey())));
    }

    /**
     * Create a shifts batch for users using employee preferences set in the application
     *
     * @param newShiftDto   Shift details
     * @param batchId       Batch UUID generated for this batch
     * @param customBatchId Custom waaw batch id generated for this batch
     * @param loggedUser    {@link UserOrganization} object for the logged-in user
     * @param notifications Empty array of {@link MultipleNotificationDto} object to store all notifications in
     */
    private void createBatchShifts(NewShiftDto newShiftDto, String batchId, String customBatchId, UserOrganization loggedUser,
                                   List<MultipleNotificationDto> notifications) {
        // Fetch all preferences for given location
        List<ShiftSchedulingPreferences> preferences = getAllPreferencesForAnOrganizationLocationRoleOrUser(
                loggedUser.getOrganizationId(), newShiftDto.getLocationId(), newShiftDto.getLocationRoleIds(),
                newShiftDto.getUserIds(), loggedUser.getAuthority()
        );
        // Get Instant for batch start and end dates
        String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                loggedUser.getLocation().getTimezone();
        Instant[] batchStartEnd = DateAndTimeUtils.getStartAndEndTimeForInstant(newShiftDto.getStart().getDate(),
                newShiftDto.getEnd().getDate(), timezone);
        // Fetch all shifts for given location in given range (plus minus 1 week each side)
        List<Shifts> existingShifts = shiftsRepository.getAllShiftsForOrganizationOrLocationBetweenDates(
                loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganizationId() : null,
                loggedUser.getAuthority().equals(Authority.MANAGER) ? loggedUser.getLocationId() : null,
                batchStartEnd[0].minus(7, ChronoUnit.DAYS), batchStartEnd[1].plus(7, ChronoUnit.DAYS)
        );
        // Fetch all holidays for given organization or location in given range
        boolean includeNextYearHolidays = !DateAndTimeUtils.isSameYear(newShiftDto.getEnd().getDate(), timezone);
        int currentYear = DateAndTimeUtils.getCurrentDate("year", timezone);
        List<OrganizationHolidays> holidays = holidayRepository.getAllForOrganizationOrLocationByYear(
                loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganizationId() : null,
                loggedUser.getAuthority().equals(Authority.MANAGER) ? loggedUser.getLocationId() : null,
                currentYear
        );
        if (includeNextYearHolidays) {
            holidays.addAll(holidayRepository.getAllForOrganizationOrLocationByYear(
                    loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganizationId() : null,
                    loggedUser.getAuthority().equals(Authority.MANAGER) ? loggedUser.getLocationId() : null,
                    currentYear + 1
            ));
        }
        // Fetch all Time offs for given organization or location in given range
        List<Requests> timeOff = requestsRepository.getOverlappingForDatesForOrganizationOrLocation(
                        batchStartEnd[0], batchStartEnd[0],
                        loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganizationId() : null,
                        loggedUser.getAuthority().equals(Authority.MANAGER) ? loggedUser.getLocationId() : null
                )
                .stream().filter(requests -> requests.getType().equals(RequestType.TIME_OFF))
                .filter(requests -> requests.getStatus().equals(RequestStatus.ACCEPTED))
                .collect(Collectors.toList());
        // Fetch all employee preference
        List<EmployeePreferencesWithUser> employeePreferenceWithUsers = getEmployeesAndPreference(newShiftDto.getLocationId(),
                newShiftDto.getLocationRoleIds(), newShiftDto.getUserIds(), loggedUser.getOrganizationId());
        // Validate and create shifts for the batch
        String currentShiftId = shiftsRepository.getLastUsedShiftId()
                .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000");
        List<Shifts> newShifts = ShiftSchedulingUtils.validateAndCreateShiftsForBatch(
                newShiftDto, loggedUser, batchId, customBatchId, timezone, existingShifts, holidays, preferences,
                employeePreferenceWithUsers, timeOff, batchStartEnd, notifications, currentShiftId, appCustomIdConfig.getLength()
        );
        if (newShifts.size() > 0) {
            shiftsRepository.saveAll(newShifts);
        } else {
            log.info("No new shifts wew created for batch: {}", newShiftDto.getShiftName());
            ShiftSchedulingUtils.addNewNotificationForShift(notifications, null, null, loggedUser,
                    "SHIFT_CREATED", customBatchId, null);
        }
    }

    /**
     * Create shifts for the given user using time provided in the request
     *
     * @param newShiftDto   Shift details
     * @param batchId       Batch UUID generated for this batch
     * @param customBatchId Custom waaw batch id generated for this batch
     * @param loggedUser    {@link UserOrganization} object for the logged-in user
     * @param notifications Empty array of {@link MultipleNotificationDto} object to store all notifications in
     */
    private void createSingleShifts(NewShiftDto newShiftDto, String batchId, String customBatchId, UserOrganization loggedUser,
                                    List<MultipleNotificationDto> notifications) {
        List<UserOrganization> users = userOrganizationRepository
                .findAllByDeleteFlagAndIdIn(false, newShiftDto.getUserIds())
                .stream().filter(user -> {
                    if (loggedUser.getAuthority().equals(Authority.MANAGER))
                        return loggedUser.getLocationId().equals(user.getLocationId());
                    else return loggedUser.getOrganizationId().equals(user.getOrganizationId());
                }).collect(Collectors.toList());
        List<ShiftSchedulingPreferences> shiftSchedulingPreferences = users.stream()
                .map(user -> ShiftSchedulingUtils.preferenceMappingFunction(user.getLocationRole(), user.getId()))
                .collect(Collectors.toList());
        AtomicReference<String> currentCustomId = new AtomicReference<>(shiftsRepository.getLastUsedShiftId()
                .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000"));
        List<Shifts> shifts = newShiftDto.getUserIds().stream()
                .map(userId -> {
                    String customId = CommonUtils.getNextCustomId(currentCustomId.get(), appCustomIdConfig.getLength());
                    UserOrganization user = users.stream().filter(user1 -> user1.getId().equals(userId))
                            .findFirst().orElse(null);
                    assert user != null;
                    Shifts shift = ShiftsMapper.shiftDtoToEntity(newShiftDto, user, batchId, customBatchId, customId, loggedUser);
                    int sameDayTimeOff = (int) requestsRepository.getOverlappingForDates(shift.getStart(), shift.getEnd(), false)
                            .stream().filter(request -> request.getUserId().equals(shift.getUserId()))
                            .filter(requests -> requests.getType().equals(RequestType.TIME_OFF))
                            .filter(requests -> requests.getStatus().equals(RequestStatus.ACCEPTED))
                            .count();
                    if (sameDayTimeOff > 0) {
                        shift.setConflicts("User has a approved timeoff present at this time");
                        ShiftSchedulingUtils.addNewNotificationForShift(notifications, shift, user, loggedUser,
                                "TIMEOFF_CONFLICT", null, null);
                    }
                    currentCustomId.set(customId);
                    List<String> conflicts = validateShift(shift, Objects.requireNonNull(shiftSchedulingPreferences
                            .stream().filter(pref -> pref.getUserId().equals(userId)).findFirst().orElse(null)));
                    if (conflicts.size() > 0) {
                        shift.setConflicts(CommonUtils.combineListToCommaSeparatedString(shift.getConflicts(), conflicts));
                        ShiftSchedulingUtils.addNewNotificationForShift(notifications, shift, user, loggedUser,
                                "CONFLICTING_SHIFTS", null, null);
                    }
                    if (shift.getShiftStatus().equals(ShiftStatus.RELEASED)) {
                        ShiftSchedulingUtils.addNewNotificationForShift(notifications, shift, user, loggedUser,
                                "SHIFT_CREATED_USER", null, null);
                    }
                    return shift;
                })
                .collect(Collectors.toList());
        shiftsRepository.saveAll(shifts);
    }

    /**
     * Will allow admin and manger to update the shift details
     *
     * @param updateShiftDto Details for shift to be updated
     */
    public void updateShift(UpdateShiftDto updateShiftDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        AtomicReference<UserOrganization> shiftsUser = new AtomicReference<>(null);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> shiftsRepository.findOneByIdAndDeleteFlag(updateShiftDto.getId(), false)
                        .flatMap(shift -> userOrganizationRepository.findOneByIdAndDeleteFlag(shift.getUserId(), false)
                                .map(user -> {
                                    if (!loggedUser.getOrganizationId().equals(user.getOrganizationId()) ||
                                            (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                                    (!loggedUser.getLocationId().equals(user.getLocationId()) ||
                                                            user.getLocationRole().isAdminRights()))) return null;
                                    shiftsUser.set(user);
                                    String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ?
                                            loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone();
                                    Instant start = DateAndTimeUtils.getDateInstant(updateShiftDto.getStart().getDate(),
                                            updateShiftDto.getStart().getTime(), timezone);
                                    Instant end = DateAndTimeUtils.getDateInstant(updateShiftDto.getEnd().getDate(),
                                            updateShiftDto.getEnd().getTime(), timezone);
                                    if (start.isBefore(Instant.now()))
                                        throw new PastValueNotDeletableException("shift");
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
                                .map(newShift -> {
                                    if (newShift.getShiftStatus().equals(ShiftStatus.RELEASED)) {
                                        NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                                                .receiverUuid(shiftsUser.get().getId())
                                                .receiverUsername(shiftsUser.get().getUsername())
                                                .language(shiftsUser.get().getLangKey())
                                                .type(NotificationType.SHIFT)
                                                .build();
                                        appNotificationService.sendApplicationNotification(MessageConstants.shiftUpdated,
                                                notificationInfo, false, newShift.getWaawShiftId(), loggedUser.getFullName());
                                    }
                                    return newShift;
                                })
                        )
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                )
                .map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftsService.class,
                        "Shift updated successfully: {}", shift));
    }

    /**
     * Delete the shift associated with given id
     *
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
                                                return shift;
                                            }
                                        }).orElseThrow(() -> new EntityNotFoundException("shift"));
                            }
                            shift.setDeleteFlag(true);
                            shift.setLastModifiedBy(user.getId());
                            return shift;
                        })
                        .map(shiftsRepository::save)
                        .map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftsService.class,
                                "Shift deleted successfully: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                ).orElseThrow(AuthenticationException::new);

    }

    /**
     * Delete all shifts associated with this batch id
     *
     * @param id for batch to be deleted
     */
    public void deleteBatch(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        AtomicBoolean pastShifts = new AtomicBoolean(false);
        AtomicBoolean notifyGlobal = new AtomicBoolean(false);
        AtomicReference<String> batchId = new AtomicReference<>(null);
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> shiftWithRoleRepository.findAllByBatchIdAndDeleteFlag(id, false)
                        .map(shifts -> {
                            batchId.set(shifts.get(0).getWaawBatchId());
                            List<ShiftWithRole> shiftsToDelete;
                            if (user.getAuthority().equals(Authority.MANAGER)) {
                                if (shifts.stream().map(ShiftWithRole::getLocationId).distinct().count() > 1)
                                    notifyGlobal.set(true);
                                shiftsToDelete = shifts.stream()
                                        .filter(shift -> shift.getLocationId().equals(user.getLocationId()) &&
                                                !shift.getLocationRole().isAdminRights())
                                        .filter(shift -> {
                                            if (shift.getStart().isBefore(Instant.now())) {
                                                pastShifts.set(true);
                                                return false;
                                            }
                                            return true;
                                        })
                                        .collect(Collectors.toList());
                            } else {
                                shiftsToDelete = shifts.stream()
                                        .filter(shift -> {
                                            if (shift.getStart().isBefore(Instant.now())) {
                                                pastShifts.set(true);
                                                return false;
                                            }
                                            return true;
                                        })
                                        .collect(Collectors.toList());
                            }
                            return shiftsToDelete;
                        })
                        .map(shifts -> shifts.stream()
                                .peek(shift -> {
                                    shift.setDeleteFlag(true);
                                    shift.setLastModifiedBy(user.getId());
                                }).collect(Collectors.toList())
                        )
                        .map(shiftWithRoleRepository::saveAll)
                        .map(shifts -> user)
                        .map(admin -> CommonUtils.logMessageAndReturnObject(admin, "info", ShiftsService.class,
                                "Batch deleted successfully: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("batch"))
                ).orElseThrow(AuthenticationException::new);
        NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                .receiverName(loggedUser.getFullName())
                .type(NotificationType.SHIFT)
                .language(loggedUser.getLangKey())
                .receiverUsername(loggedUser.getUsername())
                .receiverUuid(loggedUser.getId())
                .build();
        if (pastShifts.get()) {
            appNotificationService.sendApplicationNotification(MessageConstants.pastShiftNotDeleted, notificationInfo,
                    false, batchId.get());
        }
        if (notifyGlobal.get()) {
            appNotificationService.sendApplicationNotification(MessageConstants.batchShiftsDeletedLocation, notificationInfo,
                    false, loggedUser.getLocationRole().getName(), loggedUser.getFullName(),
                    loggedUser.getLocation().getName(), batchId.get());
        }

    }

    /**
     * Release a shift if not yet released associated with the given id
     *
     * @param id id for shift to be released
     */
    public void releaseShift(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Shifts releasedShift = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> shiftsRepository.findOneByIdAndDeleteFlag(id, false)
                        .filter(shift -> shift.getShiftStatus().equals(ShiftStatus.RELEASED))
                        .map(shift -> {
                            if (shift.getStart().isBefore(Instant.now())) {
                                throw new PastValueNotDeletableException("Shift");
                            }
                            if (!shift.getOrganizationId().equals(user.getOrganizationId()) ||
                                    (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                            !shift.getLocationId().equals(user.getLocationId()))) {
                                return null;
                            }
                            if (StringUtils.isEmpty(shift.getUserId())) {
                                throw new ShiftNotAssignedException("release");
                            }
                            shift.setShiftStatus(ShiftStatus.RELEASED);
                            shift.setLastModifiedBy(user.getId());
                            return shiftsRepository.save(shift);
                        })
                        .map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftsService.class,
                                "Shift released: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                ).orElseThrow(AuthenticationException::new);
        UserOrganization shiftsUser = userOrganizationRepository.findOneByIdAndDeleteFlag(releasedShift.getUserId(), false)
                .orElseThrow(() -> new EntityNotFoundException("user"));
        DateTimeDto shiftDate = DateAndTimeUtils.getDateTimeObjectWithFullDate(releasedShift.getStart(), shiftsUser.getLocation().getTimezone());
        NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                .receiverUsername(shiftsUser.getUsername())
                .receiverUuid(shiftsUser.getId())
                .receiverName(shiftsUser.getFullName())
                .type(NotificationType.SHIFT)
                .language(shiftsUser.getLangKey())
                .build();
        appNotificationService.sendApplicationNotification(MessageConstants.shiftAssigned, notificationInfo, false,
                shiftDate.getDate(), shiftDate.getTime());
    }

    /**
     * Release all unreleased shifts associated with the given batchId
     *
     * @param id batchId
     */
    public void releaseBatch(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        List<MultipleNotificationDto> notifications = new ArrayList<>();
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> (user.getAuthority().equals(Authority.ADMIN) ?
                        shiftDetailsRepository.findAllByOrganizationIdAndBatchIdAndDeleteFlag(user.getOrganizationId(),
                                id, false) :
                        shiftDetailsRepository.findAllByLocationIdAndBatchIdAndDeleteFlag(user.getLocationId(),
                                id, false).map(shifts -> shifts.stream()
                                .filter(shift -> !shift.getLocationRole().isAdminRights()).collect(Collectors.toList())))
                        .map(shifts -> shifts.stream().filter(shift -> shift.getShiftStatus().equals(ShiftStatus.ASSIGNED))
                                .peek(shift -> {
                                    shift.setShiftStatus(ShiftStatus.RELEASED);
                                    shift.setLastModifiedBy(user.getId());
                                    DateTimeDto shiftDate = DateAndTimeUtils.getDateTimeObjectWithFullDate(shift.getStart(),
                                            shift.getLocation().getTimezone());
                                    MultipleNotificationDto notification = MultipleNotificationDto.builder()
                                            .messageConstant(MessageConstants.shiftAssigned)
                                            .messageArguments(new String[]{shiftDate.getDate(), shiftDate.getTime()})
                                            .notificationInfo(
                                                    NotificationInfoDto.builder()
                                                            .receiverUsername(shift.getUser().getUsername())
                                                            .receiverUuid(shift.getUser().getId())
                                                            .receiverName(shift.getUser().getFullName())
                                                            .type(NotificationType.SHIFT)
                                                            .language(shift.getUser().getLangKey())
                                                            .build()
                                            )
                                            .build();
                                    notifications.add(notification);
                                })
                                .collect(Collectors.toList()))
                        .map(shiftDetailsRepository::saveAll)
                        .map(shifts -> CommonUtils.logMessageAndReturnObject(shifts, "info", ShiftsService.class,
                                "Shifts released for batch id: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("batch")));
        appNotificationService.sendMultipleApplicationNotification(notifications);
    }

    /**
     * Assign the given shift to the given user if the shift is unassigned
     *
     * @param id     id for the shift
     * @param userId id for the user to assign shift to
     */
    public void assignShift(String id, String userId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        ShiftWithRole assignedShift = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> shiftWithRoleRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(shift -> {
                            if (shift.getStart().isBefore(Instant.now())) {
                                throw new PastValueNotDeletableException("Shift");
                            }
                            if (!shift.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                            (!shift.getLocationId().equals(admin.getLocationId())
                                                    || shift.getLocationRole().isAdminRights()))) {
                                return null;
                            }
                            return userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                                    .map(user -> {
                                        if (!user.getLocationRoleId().equals(shift.getLocationRoleId())) {
                                            throw new BadRequestException("You can only assign the shift to employee with same role");
                                        }
                                        if (!shiftsRepository.findAllByUserIdAndStartBetweenAndDeleteFlag(userId,
                                                shift.getStart(), shift.getEnd(), false).isEmpty())
                                            throw new ShiftOverlappingException();
                                        shift.setUserId(user.getId());
                                        shift.setShiftStatus(ShiftStatus.RELEASED);
                                        shift.setLastModifiedBy(admin.getId());
                                        return shift;
                                    })
                                    .orElseThrow(() -> new EntityNotFoundException("user"));
                        }).map(shift -> CommonUtils.logMessageAndReturnObject(shift, "info", ShiftsService.class,
                                "Shift assigned to new user: {}", id))
                        .orElseThrow(() -> new EntityNotFoundException("shift"))
                ).orElseThrow(AuthenticationException::new);
        UserOrganization shiftsUser = userOrganizationRepository.findOneByIdAndDeleteFlag(assignedShift.getUserId(), false)
                .orElseThrow(() -> new EntityNotFoundException("user"));
        DateTimeDto shiftDate = DateAndTimeUtils.getDateTimeObjectWithFullDate(assignedShift.getStart(), shiftsUser.getLocation().getTimezone());
        NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                .receiverUsername(shiftsUser.getUsername())
                .receiverUuid(shiftsUser.getId())
                .receiverName(shiftsUser.getFullName())
                .type(NotificationType.SHIFT)
                .language(shiftsUser.getLangKey())
                .build();
        appNotificationService.sendApplicationNotification(MessageConstants.shiftAssigned, notificationInfo, false,
                shiftDate.getDate(), shiftDate.getTime());
    }

    /**
     * @param pageNo      page no for pagination, starting with 0
     * @param pageSize    no of entries per page
     * @param searchKey   key to match results with
     * @param locationId  location id for which the shifts are to be shown
     * @param roleId      location role for which shifts are to be shown
     * @param startDate   start date (yyyy-MM-dd) if filter required (both start and end date should be their or none)
     * @param endDate     end date (yyyy-MM-dd) if filter required
     * @param batchStatus batch status (FAILED, CREATED, RELEASED)
     * @param shiftStatus shift status (FAILED, CREATED, ASSIGNED, RELEASED)
     * @param shiftType   type of results required (ALL, UPCOMING, TODAYS)
     * @return Pagination object for filtered shift batch with all shifts under a batch as sub object
     */
    public PaginationDto getAllShifts(
            int pageNo, int pageSize, String searchKey, String locationId, String roleId, String startDate, String endDate,
            String batchStatus, String shiftStatus, String shiftType
    ) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                admin.getOrganization().getTimezone() : admin.getLocation().getTimezone();
        if (admin.getAuthority().equals(Authority.MANAGER)) locationId = admin.getLocationId();
        Pageable sortedPageable;
        Instant[] startEnd;
        switch (shiftType.toUpperCase()) {
            case "ALL":
                sortedPageable = PageRequest.of(pageNo, pageSize, Sort.by("created_date").descending());
                startEnd = (startDate == null || endDate == null) ? new Instant[]{null, null} :
                        DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone);
                break;
            case "UPCOMING":
                sortedPageable = PageRequest.of(pageNo, pageSize, Sort.by("start").ascending());
                startEnd = new Instant[]{Instant.now(), null};
                break;
            case "TODAYS":
                sortedPageable = PageRequest.of(pageNo, pageSize, Sort.by("start").ascending());
                startEnd = DateAndTimeUtils.getTodayInstantRange(timezone);
                break;
            default:
                throw new BadRequestException("Invalid value", "shiftType");
        }
        Page<ShiftBatchSqlDto> batchPage = getBatchPageObject(searchKey, admin.getOrganizationId(), locationId,
                roleId, startEnd, batchStatus, shiftStatus, sortedPageable);
        List<String> batchIds = batchPage.getContent().stream().map(ShiftBatchSqlDto::getId).collect(Collectors.toList());
        Map<String, List<ShiftDetails>> shifts = shiftDetailsRepository.searchAndFilterShifts(searchKey, locationId, roleId, shiftStatus,
                        null, admin.getAuthority().equals(Authority.ADMIN), startEnd[0], startEnd[1], batchIds)
                .stream()
                .sorted(Comparator.comparing(ShiftDetails::getStart))
                .collect(Collectors.groupingBy(ShiftDetails::getBatchId));
        return CommonUtils.getPaginationResponse(batchPage, ShiftsMapper::entitiesToBatchListingDto, shifts, timezone);
    }

    /**
     * @param searchKey      key to match results with
     * @param organizationId organization id for which the shifts are to be shown
     * @param locationId     location id for which the shifts are to be shown
     * @param roleId         location role for which shifts are to be shown
     * @param startEnd       Instant array of start and end date
     * @param batchStatus    batch status (FAILED, CREATED, RELEASED)
     * @param shiftStatus    shift status (FAILED, CREATED, ASSIGNED, RELEASED)
     * @param pageable       {@link Pageable} object containing page and sort details
     * @return {@link Page} object of {@link ShiftBatchSqlDto} object containing batch details
     */
    private Page<ShiftBatchSqlDto> getBatchPageObject(
            String searchKey, String organizationId, String locationId, String roleId, Instant[] startEnd,
            String batchStatus, String shiftStatus, Pageable pageable
    ) {
        if (batchStatus == null) {
            return shiftsRepository.getAllBatches(searchKey, organizationId, locationId, roleId, startEnd[0],
                    startEnd[1], shiftStatus, pageable);
        } else if (batchStatus.equalsIgnoreCase(ShiftBatchStatus.CREATED.toString())) {
            return shiftsRepository.getAllBatchesAssigned(searchKey, organizationId, locationId, roleId, startEnd[0],
                    startEnd[1], shiftStatus, pageable);
        } else {
            return shiftsRepository.getAllBatchesByStatus(searchKey, organizationId, locationId, roleId, startEnd[0],
                    startEnd[1], shiftStatus, batchStatus.toUpperCase(), pageable);
        }
    }

    /**
     * @param pageNo      page no for pagination, starting with 0
     * @param pageSize    no of entries per page
     * @param userId      userId for whom data is required
     * @param startDate   start date (yyyy-MM-dd) if filter required (both start and end date should be their or none)
     * @param endDate     end date (yyyy-MM-dd) if filter required
     * @param shiftStatus shift status (FAILED, CREATED, ASSIGNED, RELEASED)
     * @param searchKey   key to match results with
     * @return Shift details for the user whose id is passed or the logged-in user if no id is passed
     */
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
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("start").descending());
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
        Page<ShiftDetails> shiftsPage = shiftDetailsRepository.searchAndFilterShifts(searchKey,
                shiftStatus, userId, startEnd[0], startEnd[1], getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(shiftsPage, ShiftsMapper::entityToShiftDto, timezone);
    }

    /**
     * @param id id for the shift for which details are required
     * @return All shift details
     */
    public ShiftDetailsDto getShiftById(String id) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> shiftDetailsRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(shift -> {
                            if ((loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                    loggedUser.getLocationId().equals(shift.getLocation().getId())) ||
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
     * @param organizationId  organizationId
     * @param locationId      locationId
     * @param locationRoleIds locationRoleIds
     * @param userIds         userIds
     * @return List of shift scheduling preference based on one of these ids, hierarchy followed ->
     * userId, locationRoleId, locationId, organizationId
     */
    private List<ShiftSchedulingPreferences> getAllPreferencesForAnOrganizationLocationRoleOrUser(
            String organizationId, String locationId, List<String> locationRoleIds, List<String> userIds, Authority userRole
    ) {
        if (userIds != null && userIds.size() > 0) {
            return userOrganizationRepository.findAllByDeleteFlagAndIdIn(false, userIds)
                    .stream()
                    .map(user -> ShiftSchedulingUtils.preferenceMappingFunction(user.getLocationRole(), user.getId()))
                    .collect(Collectors.toList());
        } else if (locationRoleIds != null && locationRoleIds.size() > 0) {
            return locationRoleRepository.findAllByDeleteFlagAndIdIn(false, locationRoleIds)
                    .stream().map(ShiftSchedulingUtils::preferenceMappingFunction)
                    .collect(Collectors.toList());
        } else if (StringUtils.isNotEmpty(locationId)) {
            return locationRoleRepository.findAllByLocationIdAndDeleteFlag(locationId, false)
                    .stream()
                    .filter(role -> {
                        if (userRole.equals(Authority.MANAGER)) return !role.isAdminRights();
                        return true;
                    })
                    .map(ShiftSchedulingUtils::preferenceMappingFunction).collect(Collectors.toList());
        } else {
            return locationRoleRepository.findAllByOrganizationIdAndDeleteFlag(organizationId, false)
                    .stream().map(ShiftSchedulingUtils::preferenceMappingFunction).collect(Collectors.toList());
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
     * @param organization Organization for the logged in admin
     * @return A New batch id for the organizations shift batch
     * (Format: First three alphabets of organization name + a number in case of duplicate organization name present +
     * id in sequence)
     */
    public String getNewBatchId(Organization organization) {
        int orgPrefix = cachingService.getOrganizationPrefix(organization.getId(), organization.getName());
        String lastName = shiftsRepository.getLastUsedBatchId(organization.getId())
                .orElse("xxxx0000000000");
        String newNumber = String.valueOf(Integer.parseInt(lastName.substring(4)) + 1);
        String nameSuffix = StringUtils.leftPad(newNumber, appCustomIdConfig.getLength()
                - newNumber.length(), '0');
        return organization.getName().substring(0, 3) + orgPrefix + nameSuffix;
    }

}