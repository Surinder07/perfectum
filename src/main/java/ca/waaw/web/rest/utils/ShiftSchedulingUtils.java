package ca.waaw.web.rest.utils;

import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.user.EmployeePreferencesWithUser;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.*;
import ca.waaw.dto.appnotifications.MultipleNotificationDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftSchedulingPreferences;
import ca.waaw.dto.userdtos.EmployeePreferencesDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.shift.ShiftType;
import ca.waaw.web.rest.errors.exceptions.application.ShiftOverlappingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ShiftSchedulingUtils {

    private static final Logger log = LogManager.getLogger(ShiftSchedulingUtils.class);

    /**
     * @param shift                      new shift entity being created
     * @param shiftSchedulingPreferences scheduling preferences for location role
     * @param shiftsToCheck              List of shifts for allowed consecutive day number in past and future
     * @return List of any conflicts between shift scheduling and preferences
     */
    public static List<String> validateShift(Shifts shift, ShiftSchedulingPreferences shiftSchedulingPreferences,
                                             List<Shifts> shiftsToCheck) {
        List<String> conflictReasons = new ArrayList<>();
        List<Shifts> existingSameDatShifts = shiftsToCheck.stream()
                .filter(tempShift -> DateAndTimeUtils.isInstantSameDayAsAnotherInstant(tempShift.getStart(), shift.getStart()))
                .collect(Collectors.toList());
        long shiftDurationInSeconds = shift.getEnd().getEpochSecond() - shift.getStart().getEpochSecond();
        try {
            long shiftSecondsForSameDay = checkShiftOverlapAndReturnShiftHours(existingSameDatShifts,
                    shift.getStart(), shift.getEnd());
            // Check for maximum and minimum hours per day of shift
            if ((shiftDurationInSeconds + shiftSecondsForSameDay) > shiftSchedulingPreferences.getTotalMinutesPerDayMax() * 60L) {
                conflictReasons.add("Maximum hours for shift per day exceeded.");
                log.warn("Maximum hours for shift per day exceeded for shift {}", shift.getId());
            } else if ((shiftDurationInSeconds + shiftSecondsForSameDay) < shiftSchedulingPreferences.getTotalMinutesPerDayMin() * 60L) {
                conflictReasons.add("Minimum hours for shift per day not reached.");
                log.warn("Minimum hours for shift per day not reached for shift {}", shift.getId());
            }
            // Check for Consecutive Days
            validateMaximumConsecutiveWorkDays(shiftsToCheck, shift, conflictReasons, shiftSchedulingPreferences);
            // Check for minimum gap in two shifts
            validateGapBetweenTwoShifts(shiftsToCheck, shift, conflictReasons, shiftSchedulingPreferences);
        } catch (ShiftOverlappingException e) {
            log.error("There is an existing overlapping shift for the shift {}", shift);
            shift.setShiftStatus(ShiftStatus.FAILED);
            shift.setFailureReason("An existing shift overlaps with this shift.");
        }
        return conflictReasons;
    }

    /**
     * Will throw an error if shifts are overlapping on same day
     *
     * @param sameDayShifts list of shifts on same day
     * @param shiftStart    shift start dateTime for the shift to be checked
     * @param shiftEnd      shift end dateTime for the shift to be checked
     * @return seconds of shift on this day if any present
     */
    public static long checkShiftOverlapAndReturnShiftHours(List<Shifts> sameDayShifts, Instant shiftStart, Instant shiftEnd) {
        MutableBoolean error = new MutableBoolean(false);
        long totalWorkSeconds = Optional.of(sameDayShifts).map(shifts -> shifts.stream()
                        .filter(shift -> StringUtils.isNotEmpty(shift.getUserId()))
                        .mapToLong(shift -> {
                            if (DateAndTimeUtils.isInstantBetweenInstants(shift.getStart(), shiftStart, shiftEnd) ||
                                    DateAndTimeUtils.isInstantBetweenInstants(shift.getEnd(), shiftStart, shiftEnd)) {
                                error.setTrue();
                            }
                            return shift.getEnd().getEpochSecond() - shift.getStart().getEpochSecond();
                        }).sum())
                .orElse(0L);
        if (error.isTrue()) throw new ShiftOverlappingException();
        return totalWorkSeconds;
    }

    /**
     * If minimum gap between two shifts is not according to the preferences, it will add the reason in the list
     *
     * @param shiftsToCheck              List of shifts for allowed consecutive day number in past and future
     * @param shift                      new shift entity being created
     * @param conflictReasons            list of conflict reasons
     * @param shiftSchedulingPreferences scheduling preferences for location role
     */
    public static void validateGapBetweenTwoShifts(List<Shifts> shiftsToCheck, Shifts shift, List<String> conflictReasons,
                                                   ShiftSchedulingPreferences shiftSchedulingPreferences) {
        List<Shifts> oneDayPastAndFutureShifts = shiftsToCheck.stream()
                .filter(tempShift -> DateAndTimeUtils.isInstantBetweenInstants(tempShift.getStart(),
                        shift.getStart().minus(1, ChronoUnit.DAYS), shift.getStart().plus(1, ChronoUnit.DAYS)))
                .collect(Collectors.toList());
        boolean shiftDifferenceFailed = oneDayPastAndFutureShifts.stream()
                .anyMatch(tempShift -> {
                    if (tempShift.getStart().isBefore(shift.getStart())) {
                        return tempShift.getEnd().getEpochSecond() - shift.getStart().getEpochSecond() <
                                shiftSchedulingPreferences.getMinMinutesBetweenShifts() * 60L;
                    } else {
                        return shift.getEnd().getEpochSecond() - tempShift.getStart().getEpochSecond() <
                                shiftSchedulingPreferences.getMinMinutesBetweenShifts() * 60L;
                    }
                });
        if (shiftDifferenceFailed) {
            conflictReasons.add("Minimum hours between two shifts is not being followed.");
            log.warn("Minimum hours between two shifts is not being followed for shift {}", shift.getId());
        }
    }

    /**
     * If max consecutive work days are not according to the preferences, it will add the reason in the list
     *
     * @param shiftsToCheck              List of shifts for allowed consecutive day number in past and future
     * @param shift                      new shift entity being created
     * @param conflictReasons            list of conflict reasons
     * @param shiftSchedulingPreferences scheduling preferences for location role
     */
    public static void validateMaximumConsecutiveWorkDays(List<Shifts> shiftsToCheck, Shifts shift, List<String> conflictReasons,
                                                          ShiftSchedulingPreferences shiftSchedulingPreferences) {
        if (shiftsToCheck.size() > shiftSchedulingPreferences.getMaxConsecutiveWorkDays()) {
            AtomicReference<Instant> startCompareDate = new AtomicReference<>(shift.getStart()
                    .minus(shiftSchedulingPreferences.getMaxConsecutiveWorkDays(), ChronoUnit.DAYS));
            MutableBoolean conflict = new MutableBoolean(false);
            MutableInt consecutiveDays = new MutableInt(0);
            IntStream.range(0, (shiftSchedulingPreferences.getMaxConsecutiveWorkDays() * 2) - 1)
                    .forEach(index -> {
                        if (shiftsToCheck.stream().filter(tempShift -> DateAndTimeUtils
                                .isInstantSameDayAsAnotherInstant(tempShift.getStart(), startCompareDate.get())).count() > 0L) {
                            consecutiveDays.add(1);
                        } else consecutiveDays.setValue(0);

                        if (consecutiveDays.getValue() > shiftSchedulingPreferences.getMaxConsecutiveWorkDays()) {
                            conflict.isTrue();
                        }
                        startCompareDate.set(startCompareDate.get().plus(1, ChronoUnit.DAYS));
                    });
            if (conflict.isTrue()) {
                conflictReasons.add("Maximum consecutive days are not being followed.");
                log.warn("Maximum consecutive days are not being followed for shift {}", shift.getId());
            }
        }
    }

    /**
     * @param preferences location role scheduling preferences
     * @param dto         new employee preferences
     * @return true if preferences are not being followed
     */
    public static boolean validateEmployeePreference(ShiftSchedulingPreferences preferences, EmployeePreferencesDto dto) {
        List<String> allStartTimes = Stream.of(dto.getMondayStartTime(), dto.getTuesdayStartTime(), dto.getWednesdayStartTime(),
                        dto.getThursdayStartTime(), dto.getFridayStartTime(), dto.getSaturdayStartTime(), dto.getSundayStartTime())
                .filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        List<Float> allShiftDurations = Stream.of(
                        getTimeDifference(dto.getMondayStartTime(), dto.getMondayEndTime()),
                        getTimeDifference(dto.getTuesdayEndTime(), dto.getTuesdayStartTime()),
                        getTimeDifference(dto.getWednesdayStartTime(), dto.getWednesdayEndTime()),
                        getTimeDifference(dto.getThursdayStartTime(), dto.getThursdayStartTime()),
                        getTimeDifference(dto.getFridayStartTime(), dto.getFridayEndTime()),
                        getTimeDifference(dto.getSaturdayStartTime(), dto.getSaturdayEndTime()),
                        getTimeDifference(dto.getSundayStartTime(), dto.getSundayEndTime())
                )
                .filter(duration -> duration != 0F).collect(Collectors.toList());
        return allShiftDurations.stream().anyMatch(duration -> duration < preferences.getTotalMinutesPerDayMin() ||
                duration > preferences.getTotalMinutesPerDayMax()) || allStartTimes.size() > preferences.getMaxConsecutiveWorkDays()
                || checkTimeDifferenceForEachDay(dto, preferences.getMinMinutesBetweenShifts());
    }

    /**
     * @param dto                  employee preferences to be updated
     * @param minMinutesDifference min difference between two shifts required
     * @return true if min difference between two shifts is not followed
     */
    public static boolean checkTimeDifferenceForEachDay(EmployeePreferencesDto dto, int minMinutesDifference) {
        return checkTimeDifferenceForTwoDays(dto.getMondayStartTime(), dto.getSundayEndTime(), dto.getTuesdayStartTime(), minMinutesDifference)
                || checkTimeDifferenceForTwoDays(dto.getTuesdayStartTime(), dto.getSundayEndTime(), dto.getWednesdayStartTime(), minMinutesDifference)
                || checkTimeDifferenceForTwoDays(dto.getWednesdayStartTime(), dto.getSundayEndTime(), dto.getThursdayStartTime(), minMinutesDifference)
                || checkTimeDifferenceForTwoDays(dto.getThursdayStartTime(), dto.getSundayEndTime(), dto.getFridayStartTime(), minMinutesDifference)
                || checkTimeDifferenceForTwoDays(dto.getFridayStartTime(), dto.getSundayEndTime(), dto.getSaturdayStartTime(), minMinutesDifference)
                || checkTimeDifferenceForTwoDays(dto.getSaturdayStartTime(), dto.getSundayEndTime(), dto.getSundayStartTime(), minMinutesDifference)
                || checkTimeDifferenceForTwoDays(dto.getSundayStartTime(), dto.getSundayEndTime(), dto.getMondayStartTime(), minMinutesDifference);
    }

    /**
     * @param firstDayStart       first day start time (24:00 hours pattern)
     * @param firstDayEnd         working hours for first day
     * @param secondDayStart      second day start time (24:00 hours pattern)
     * @param minMinuteDifference min difference between two shifts required
     * @return true if min difference between two shifts is not followed
     */
    private static boolean checkTimeDifferenceForTwoDays(String firstDayStart, String firstDayEnd, String secondDayStart,
                                                         int minMinuteDifference) {
        if (StringUtils.isNotEmpty(firstDayStart) && StringUtils.isNotEmpty(secondDayStart)) {
            return DateAndTimeUtils.getTimeDifference(firstDayStart, secondDayStart, 1) <
                    (minMinuteDifference + getTimeDifference(firstDayEnd, firstDayStart));
        }
        return false;
    }

    private static float getTimeDifference(String time1, String time2) {
        return StringUtils.isNotEmpty(time1) && StringUtils.isNotEmpty(time2) ?
                ((float) Duration.between(LocalTime.parse(time1), LocalTime.parse(time2)).toMinutes()) : 0;
    }

    /**
     * @param date        date for which shift times are required
     * @param preferences employee preferences
     * @param timezone    timezone for location
     * @return start and end time for shift according to preferences for the day that will be on given date
     */
    public static Instant[] getStartEndFromEmployeePreference(Instant date, EmployeePreferencesWithUser preferences, String timezone) {
        DayOfWeek day = date.atZone(ZoneId.of(timezone)).getDayOfWeek();
        String startTime = null;
        float workingMinutes = 0;
        switch (day) {
            case MONDAY:
                startTime = preferences.getMondayStartTime();
                workingMinutes = getTimeDifference(preferences.getMondayStartTime(), preferences.getMondayEndTime());
                break;
            case TUESDAY:
                startTime = preferences.getTuesdayStartTime();
                workingMinutes = getTimeDifference(preferences.getTuesdayStartTime(), preferences.getTuesdayEndTime());
                break;
            case WEDNESDAY:
                startTime = preferences.getWednesdayStartTime();
                workingMinutes = getTimeDifference(preferences.getWednesdayStartTime(), preferences.getWednesdayEndTime());
                break;
            case THURSDAY:
                startTime = preferences.getThursdayStartTime();
                workingMinutes = getTimeDifference(preferences.getThursdayStartTime(), preferences.getThursdayEndTime());
                break;
            case FRIDAY:
                startTime = preferences.getFridayStartTime();
                workingMinutes = getTimeDifference(preferences.getFridayStartTime(), preferences.getFridayEndTime());
                break;
            case SATURDAY:
                startTime = preferences.getSaturdayStartTime();
                workingMinutes = getTimeDifference(preferences.getSaturdayStartTime(), preferences.getSaturdayEndTime());
                break;
            case SUNDAY:
                startTime = preferences.getSundayStartTime();
                workingMinutes = getTimeDifference(preferences.getSundayStartTime(), preferences.getSundayEndTime());
                break;
        }
        if (StringUtils.isEmpty(startTime)) return null;
        String[] startTimeArray = startTime.split(":");
        Instant shiftStart = date.atZone(ZoneId.of(timezone)).withHour(Integer.parseInt(startTimeArray[0]))
                .withMinute(Integer.parseInt(startTimeArray[1])).withSecond(0).withNano(0).toInstant();
        return new Instant[]{shiftStart, shiftStart.plus((long) (workingMinutes), ChronoUnit.MINUTES)};
    }

    /**
     * @param shiftDetails                DTO containing all shift details
     * @param loggedUser                  {@link UserOrganization object for logged-in user}
     * @param batchId                     Batch uuid generated for this batch
     * @param customBatchId               WaaW batch id generated for this batch
     * @param timezone                    Timezone for the logged-in user
     * @param existingShifts              List of existing shifts in this date range
     * @param holidays                    List of organization holidays
     * @param preferences                 All shift preferences for target employees
     * @param employeePreferenceWithUsers All employees list with preferences
     * @param timeOff                     List of all allowed time off requests within this date range
     * @param startEndDates               Start and End date Instants converted to logged-in user's timezone
     * @param notifications               An empty list of notifications to add notifications to (They will be saved in bulk later)
     * @param currentWaawShiftId          Last used Shift id for WAAW
     * @param customIdLength              Allowed length for the custom id
     * @return A list of new Shift objects created for this batch
     */
    public static List<Shifts> validateAndCreateShiftsForBatch(
            NewShiftDto shiftDetails, UserOrganization loggedUser, String batchId, String customBatchId, String timezone,
            List<Shifts> existingShifts, List<OrganizationHolidays> holidays, List<ShiftSchedulingPreferences> preferences,
            List<EmployeePreferencesWithUser> employeePreferenceWithUsers, List<Requests> timeOff, Instant[] startEndDates,
            List<MultipleNotificationDto> notifications, String currentWaawShiftId, int customIdLength
    ) {
        List<Shifts> newShifts = new ArrayList<>();
        try {
            Instant startDate = startEndDates[0];
            AtomicReference<String> customId = new AtomicReference<>(currentWaawShiftId);
            while (startDate.isBefore(startEndDates[1])) {
                // Check if today is a holiday and create shifts if it isn't
                Instant finalStartDate = startDate;
                boolean isHoliday = holidays.stream().anyMatch(tempHoliday -> {
                    String[] splitDate = finalStartDate.atZone(ZoneId.of(timezone)).toString().split("T")[0].split("/");
                    return tempHoliday.getYear() == Integer.parseInt(splitDate[0]) && tempHoliday.getMonth() ==
                            Integer.parseInt(splitDate[1]) && tempHoliday.getDate() == Integer.parseInt(splitDate[2]);
                });
                if (!isHoliday) {
                    List<Shifts> newShiftsForOneDay = createShiftsForOneDayOfBatch(startEndDates, startDate, existingShifts,
                            preferences, employeePreferenceWithUsers, timeOff, timezone, loggedUser, notifications,
                            shiftDetails.isInstantRelease(), batchId, customBatchId, shiftDetails.getShiftName(),
                            customId, customIdLength);
                    newShifts.addAll(newShiftsForOneDay);
                } else {
                    addNewNotificationForShift(notifications, null, null, loggedUser,
                            "HOLIDAY_CONFLICT", customBatchId, null);
                    log.info("Shifts for date {} are being skipped as there is a holiday on that date",
                            DateAndTimeUtils.getDateTimeObjectWithFullDate(startDate, timezone).getDate());
                }
                startDate = startDate.plus(1, ChronoUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("Exception while saving shifts for batch: {}", batchId, e);
        }
        return newShifts;
    }

    /**
     * For any employee for whom preferences are not set, a notification will be sent to the admin.
     *
     * @param startEndDate                Date range for the entire batch
     * @param date                        Date for which shifts are being created
     * @param existingShifts              List of existing shifts
     * @param preferences                 All shift preferences for this location
     * @param employeePreferenceWithUsers All employees list with preferences
     * @param timeOff                     List of all allowed time off requests within this date range
     * @param timezone                    Timezone for the logged-in user
     * @param loggedUser                  {@link UserOrganization object for logged-in user}
     * @param notifications               An empty list of notifications to add notifications to (They will be saved in bulk later)
     * @param instantRelease              Boolean value for if we should instantly release shift to the user
     * @param batchId                     Batch uuid generated for this batch
     * @param customBatchId               WaaW batch id generated for this batch
     * @param batchName                   Batch name provided in the DTO
     * @param currentWaawShiftId          Last used Shift id for WAAW
     * @param customIdLength              Allowed length for the custom id
     * @return List of new shifts created for this single date
     */
    private static List<Shifts> createShiftsForOneDayOfBatch(
            Instant[] startEndDate, Instant date, List<Shifts> existingShifts, List<ShiftSchedulingPreferences> preferences,
            List<EmployeePreferencesWithUser> employeePreferenceWithUsers, List<Requests> timeOff, String timezone,
            UserOrganization loggedUser, List<MultipleNotificationDto> notifications, boolean instantRelease,
            String batchId, String customBatchId, String batchName, AtomicReference<String> currentWaawShiftId, int customIdLength
    ) {
        List<Shifts> newShifts = new ArrayList<>();
        Map<String, List<ShiftSchedulingPreferences>> locationRolePreference = preferences.stream()
                .collect(Collectors.groupingBy(ShiftSchedulingPreferences::getLocationRoleId, Collectors.toList()));
        Map<String, List<Shifts>> shiftsToCheckPerLocationRole = getShiftsToCheckPerLocationRole(date, existingShifts,
                locationRolePreference);
        employeePreferenceWithUsers.forEach(preference -> {
            // id belongs to Employee preference table, if it is empty preferences are not set
            if (StringUtils.isEmpty(preference.getId())) {
                addNewNotificationForShift(notifications, null, null, loggedUser,
                        "MISSING_PREFERENCE", customBatchId, preference);
                log.info("User: {} found with no preferences.", preference.getUserId());
            } else {
                try {
                    Instant[] shiftDuration = ShiftSchedulingUtils.getStartEndFromEmployeePreference(date, preference, timezone);
                    boolean sameDayExistingShit = existingShifts.stream()
                            .anyMatch(shift -> DateAndTimeUtils.isInstantSameDayAsAnotherInstant(date, shift.getStart()));
                    if (shiftDuration != null) {
                        Shifts newShift = new Shifts();
                        newShift.setBatchId(batchId);
                        newShift.setWaawBatchId(customBatchId);
                        newShift.setWaawShiftId(CommonUtils.getNextCustomId(currentWaawShiftId.get(), customIdLength));
                        newShift.setBatchName(batchName);
                        newShift.setBatchStart(startEndDate[0]);
                        newShift.setBatchEnd(startEndDate[1]);
                        newShift.setUserId(preference.getUserId());
                        newShift.setStart(shiftDuration[0]);
                        newShift.setEnd(shiftDuration[1]);
                        newShift.setShiftType(ShiftType.RECURRING);
                        newShift.setShiftStatus(instantRelease ? ShiftStatus.RELEASED : ShiftStatus.ASSIGNED);
                        newShift.setOrganizationId(preference.getOrganizationId());
                        newShift.setLocationId(preference.getLocationId());
                        newShift.setLocationRoleId(preference.getLocationRoleId());
                        newShift.setCreatedBy(loggedUser.getId());
                        currentWaawShiftId.set(newShift.getWaawShiftId());
                        boolean isTimeoff = timeOff.stream()
                                .filter(request -> request.getUserId().equals(preference.getUserId()))
                                .anyMatch(request -> (shiftDuration[0].isAfter(request.getStart()) && shiftDuration[0].isBefore(request.getEnd())) ||
                                        (shiftDuration[1].isAfter(request.getStart()) && shiftDuration[1].isBefore(request.getEnd())) ||
                                        (request.getStart().isAfter(shiftDuration[0]) && request.getStart().isBefore(shiftDuration[1])) ||
                                        (request.getEnd().isAfter(shiftDuration[0]) && request.getEnd().isBefore(shiftDuration[1])));
                        List<Shifts> checkShifts = shiftsToCheckPerLocationRole.get(preference.getLocationRoleId())
                                .stream().filter(shifts -> shifts.getUserId().equalsIgnoreCase(preference.getUserId()))
                                .collect(Collectors.toList());
                        List<String> conflicts = validateShift(newShift, locationRolePreference.get(preference
                                .getLocationRoleId()).get(0), checkShifts);
                        if (conflicts.size() > 0) {
                            newShift.setConflicts(CommonUtils.combineListToCommaSeparatedString(newShift.getConflicts(), conflicts));
                            addNewNotificationForShift(notifications, newShift, null, loggedUser,
                                    "CONFLICTING_SHIFTS", null, preference);
                        }
                        if (sameDayExistingShit) {
                            newShift.setShiftStatus(ShiftStatus.FAILED);
                            newShift.setFailureReason("An existing shift overlaps with this shift.");
                            log.warn("A shift already exist on same day. " +
                                    "Skipping shift for user {}, on date {}", preference.getUserId(), date);
                        }
                        if (isTimeoff) {
                            newShift.setShiftStatus(ShiftStatus.FAILED);
                            newShift.setFailureReason("A time off request is already approved for this time.");
                            addNewNotificationForShift(notifications, newShift, null, loggedUser,
                                    "TIMEOFF_CONFLICT", null, preference);
                        }
                        if (newShift.getShiftStatus().equals(ShiftStatus.RELEASED)) {
                            ShiftSchedulingUtils.addNewNotificationForShift(notifications, newShift, null, loggedUser,
                                    "SHIFT_CREATED_USER", null, preference);
                        }
                        newShifts.add(newShift);
                        log.info("New shift entity created for user {}: {}", preference.getUserId(), newShift);
                    } else {
                        log.warn("Skipping shift for user {} on date {} as preference is not set for this day.",
                                preference.getUserId(), date);
                    }
                } catch (Exception e) {
                    log.error("Exception while creating shift for user: {} at date {}",
                            preference.getUserId(), date, e);
                }
            }
        });
        return newShifts;
    }

    /**
     * @param date                   date for which shifts are being created
     * @param existingShifts         list of existing shifts
     * @param locationRolePreference map of shift preferences per location role
     * @return Map of list of shifts to check for consecutive days per location role
     */
    private static Map<String, List<Shifts>> getShiftsToCheckPerLocationRole(Instant date, List<Shifts> existingShifts,
                                                                             Map<String, List<ShiftSchedulingPreferences>> locationRolePreference) {
        Map<String, List<Shifts>> shiftsPerLocationRole = new HashMap<>();
        locationRolePreference.forEach((locationRoleId, preferenceList) -> {
            int maxConsecutiveDay = preferenceList.get(0).getMaxConsecutiveWorkDays();
            shiftsPerLocationRole.put(locationRoleId, getShiftsToCheck(date, existingShifts, maxConsecutiveDay));
        });
        return shiftsPerLocationRole;
    }

    /**
     * @param date               date for which shifts are being created
     * @param existingShifts     list of existing shifts
     * @param maxConsecutiveDays max consecutive days set in preferences
     * @return List of shifts to check for consecutive days
     */
    private static List<Shifts> getShiftsToCheck(Instant date, List<Shifts> existingShifts, int maxConsecutiveDays) {
        Instant[] dateRangeForConsecutiveCheck = DateAndTimeUtils.getStartAndEndTimeForInstant(date
                .minus(maxConsecutiveDays, ChronoUnit.DAYS), maxConsecutiveDays * 2);
        return existingShifts.stream().filter(shift ->
                shift.getStart().isAfter(dateRangeForConsecutiveCheck[0]) || shift.getStart().equals(dateRangeForConsecutiveCheck[0]) &&
                        shift.getStart().isBefore(dateRangeForConsecutiveCheck[1]) || shift.getStart().equals(dateRangeForConsecutiveCheck[1])
        ).collect(Collectors.toList());
    }

    /**
     * Mainly used in {@link ca.waaw.web.rest.service.ShiftsService} method getAllPreferencesForALocationOrUser
     *
     * @param locationRole location role info
     * @return Shift scheduling preferences for this role
     */
    public static ShiftSchedulingPreferences preferenceMappingFunction(LocationRole locationRole) {
        if (locationRole == null) return null;
        ShiftSchedulingPreferences preferences = new ShiftSchedulingPreferences();
        preferences.setLocationRoleId(locationRole.getId());
        preferences.setTotalMinutesPerDayMin(locationRole.getTotalMinutesPerDayMin());
        preferences.setTotalMinutesPerDayMax(locationRole.getTotalMinutesPerDayMax());
        preferences.setTotalMinutesPerDayMin(locationRole.getTotalMinutesPerDayMin());
        preferences.setMaxConsecutiveWorkDays(locationRole.getMaxConsecutiveWorkDays());
        return preferences;
    }

    /**
     * Mainly used in {@link ca.waaw.web.rest.service.ShiftsService} method getAllPreferencesForALocationOrUser
     *
     * @param locationRole location role info
     * @param userId       userId for which preference is fetched
     * @return Shift scheduling preferences for this role
     */
    public static ShiftSchedulingPreferences preferenceMappingFunction(LocationRole locationRole, String userId) {
        if (locationRole == null) return null;
        ShiftSchedulingPreferences preferences = preferenceMappingFunction(locationRole);
        preferences.setUserId(userId);
        return preferences;
    }

    public static void addNewNotificationForShift(List<MultipleNotificationDto> notifications, Shifts newShift, UserOrganization user,
                                                  UserOrganization admin, String notificationType, String batchId,
                                                  EmployeePreferencesWithUser preference) {
        String[] messageConstant;
        String[] messageArguments;
        UserOrganization receiver;
        switch (notificationType.toUpperCase()) {
            case "TIMEOFF_CONFLICT":
                receiver = admin;
                messageConstant = MessageConstants.shiftTimeoffOverlap;
                messageArguments = new String[]{newShift.getWaawShiftId(), user == null ?
                        preference.getFullName() : user.getFullName()};
                break;
            case "SHIFT_CREATED_USER":
                receiver = user;
                DateTimeDto shiftDate = DateAndTimeUtils.getDateTimeObjectWithFullDate(newShift.getStart(), user.getLocation().getTimezone());
                messageConstant = MessageConstants.shiftAssigned;
                messageArguments = new String[]{shiftDate.getDate(), shiftDate.getTime()};
                break;
            case "SHIFT_CREATED":
                receiver = admin;
                messageConstant = MessageConstants.shiftCreated;
                messageArguments = new String[]{batchId};
                break;
            case "CONFLICTING_SHIFTS":
                receiver = admin;
                messageConstant = MessageConstants.shiftConflicted;
                messageArguments = new String[]{newShift.getWaawShiftId(), user == null ?
                        preference.getFullName() : user.getFullName()};
                break;
            case "HOLIDAY_CONFLICT":
                receiver = admin;
                messageConstant = MessageConstants.shiftHolidayOverlap;
                messageArguments = new String[]{batchId};
                break;
            case "MISSING_PREFERENCE":
                receiver = admin;
                messageConstant = MessageConstants.missingEmployeePreference;
                messageArguments = new String[]{batchId, preference.getFullName()};
                break;
            case "NO_SHIFTS":
                receiver = admin;
                messageConstant = MessageConstants.noShiftsCreated;
                messageArguments = new String[]{batchId};
                break;
            default:
                return;
        }
        MultipleNotificationDto notification = MultipleNotificationDto.builder()
                .messageConstant(messageConstant)
                .messageArguments(messageArguments)
                .notificationInfo(
                        NotificationInfoDto.builder()
                                .receiverName(receiver == null ? preference.getFullName() : receiver.getFullName())
                                .type(NotificationType.SHIFT)
                                .language(receiver == null ? preference.getLangKey() : receiver.getLangKey())
                                .receiverUsername(receiver == null ? preference.getUsername() : receiver.getUsername())
                                .receiverUuid(receiver == null ? preference.getUserId() : receiver.getId())
                                .build()
                )
                .build();
        notifications.add(notification);
    }

}