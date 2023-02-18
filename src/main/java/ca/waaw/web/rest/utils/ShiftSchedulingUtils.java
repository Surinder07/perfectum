package ca.waaw.web.rest.utils;

import ca.waaw.domain.*;
import ca.waaw.domain.joined.EmployeePreferencesWithUser;
import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import ca.waaw.web.rest.errors.exceptions.application.ShiftOverlappingException;
import ca.waaw.web.rest.service.ShiftSchedulingService;
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
            if ((shiftDurationInSeconds + shiftSecondsForSameDay) > shiftSchedulingPreferences.getTotalHoursPerDayMax() * 3600L) {
                conflictReasons.add("Maximum hours for shift per day exceeded.");
                log.warn("Maximum hours for shift per day exceeded for shift {}", shift.getId());
            } else if ((shiftDurationInSeconds + shiftSecondsForSameDay) < shiftSchedulingPreferences.getTotalHoursPerDayMin() * 3600L) {
                conflictReasons.add("Minimum hours for shift per day not reached.");
                log.warn("Minimum hours for shift per day not reached for shift {}", shift.getId());
            }
            // Check for Consecutive Days
            validateMaximumConsecutiveWorkDays(shiftsToCheck, shift, conflictReasons, shiftSchedulingPreferences);
            // Check for minimum gap in two shifts
            validateGapBetweenTwoShifts(shiftsToCheck, shift, conflictReasons, shiftSchedulingPreferences);
        } catch (ShiftOverlappingException e) {
            shift.setShiftStatus(ShiftStatus.FAILED);
            shift.setNotes("An existing shift overlaps with this shift.");
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
                                shiftSchedulingPreferences.getMinHoursBetweenShifts() * 3600L;
                    } else {
                        return shift.getEnd().getEpochSecond() - tempShift.getStart().getEpochSecond() <
                                shiftSchedulingPreferences.getMinHoursBetweenShifts() * 3600L;
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
        return allShiftDurations.stream().anyMatch(duration -> duration < preferences.getTotalHoursPerDayMin() ||
                duration > preferences.getTotalHoursPerDayMax()) || allStartTimes.size() > preferences.getMaxConsecutiveWorkDays()
                || checkTimeDifferenceForEachDay(dto, preferences.getMinHoursBetweenShifts());
    }

    /**
     * @param dto               employee preferences to be updated
     * @param minHourDifference min difference between two shifts required
     * @return true if min difference between two shifts is not followed
     */
    public static boolean checkTimeDifferenceForEachDay(EmployeePreferencesDto dto, int minHourDifference) {
        return checkTimeDifferenceForTwoDays(dto.getMondayStartTime(), dto.getSundayEndTime(), dto.getTuesdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getTuesdayStartTime(), dto.getSundayEndTime(), dto.getWednesdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getWednesdayStartTime(), dto.getSundayEndTime(), dto.getThursdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getThursdayStartTime(), dto.getSundayEndTime(), dto.getFridayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getFridayStartTime(), dto.getSundayEndTime(), dto.getSaturdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getSaturdayStartTime(), dto.getSundayEndTime(), dto.getSundayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getSundayStartTime(), dto.getSundayEndTime(), dto.getMondayStartTime(), minHourDifference);
    }

    /**
     * @param firstDayStart     first day start time (24:00 hours pattern)
     * @param firstDayEnd       working hours for first day
     * @param secondDayStart    second day start time (24:00 hours pattern)
     * @param minHourDifference min difference between two shifts required
     * @return true if min difference between two shifts is not followed
     */
    private static boolean checkTimeDifferenceForTwoDays(String firstDayStart, String firstDayEnd, String secondDayStart,
                                                         int minHourDifference) {
        if (StringUtils.isNotEmpty(firstDayStart) && StringUtils.isNotEmpty(secondDayStart)) {
            return DateAndTimeUtils.getTimeDifference(firstDayStart, secondDayStart, 1) <
                    (minHourDifference + getTimeDifference(firstDayEnd, firstDayStart));
        }
        return false;
    }

    private static float getTimeDifference(String time1, String time2) {
        return StringUtils.isNotEmpty(time1) && StringUtils.isNotEmpty(time2) ?
                ((float) Duration.between(LocalTime.parse(time1), LocalTime.parse(time2)).toMinutes() / 60) : 0;
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
        float workingHours = 0;
        switch (day) {
            case MONDAY:
                startTime = preferences.getMondayStartTime();
                workingHours = getTimeDifference(preferences.getMondayStartTime(), preferences.getMondayEndTime());
                break;
            case TUESDAY:
                startTime = preferences.getTuesdayStartTime();
                workingHours = getTimeDifference(preferences.getTuesdayStartTime(), preferences.getTuesdayEndTime());
                break;
            case WEDNESDAY:
                startTime = preferences.getWednesdayStartTime();
                workingHours = getTimeDifference(preferences.getWednesdayStartTime(), preferences.getWednesdayEndTime());
                break;
            case THURSDAY:
                startTime = preferences.getThursdayStartTime();
                workingHours = getTimeDifference(preferences.getThursdayStartTime(), preferences.getThursdayEndTime());
                break;
            case FRIDAY:
                startTime = preferences.getFridayStartTime();
                workingHours = getTimeDifference(preferences.getFridayStartTime(), preferences.getFridayEndTime());
                break;
            case SATURDAY:
                startTime = preferences.getSaturdayStartTime();
                workingHours = getTimeDifference(preferences.getSaturdayStartTime(), preferences.getSaturdayEndTime());
                break;
            case SUNDAY:
                startTime = preferences.getSundayStartTime();
                workingHours = getTimeDifference(preferences.getSundayStartTime(), preferences.getSundayEndTime());
                break;
        }
        if (StringUtils.isEmpty(startTime)) return null;
        String[] startTimeArray = startTime.split(":");
        Instant shiftStart = date.atZone(ZoneId.of(timezone)).withHour(Integer.parseInt(startTimeArray[0]))
                .withMinute(Integer.parseInt(startTimeArray[1])).withSecond(0).withNano(0).toInstant();
        return new Instant[]{shiftStart, shiftStart.plus((long) (workingHours * 60), ChronoUnit.MINUTES)};
    }

    /**
     * Will create shifts for all employees between batch dates based on employee preferences
     *
     * @param batch                       batch details
     * @param existingShifts              list of existing shifts
     * @param holidays                    list of organization holidays
     * @param preferences                 all shift preferences for this location
     * @param employeePreferenceWithUsers all employees list with preferences
     * @param timezone                    timezone for this location
     */
    public static List<Shifts> validateAndCreateShiftsForBatch(ShiftsBatch batch, List<Shifts> existingShifts, List<OrganizationHolidays> holidays,
                                                               List<ShiftSchedulingPreferences> preferences, List<EmployeePreferencesWithUser> employeePreferenceWithUsers,
                                                               String timezone, List<String> employeeWithoutPreferences,
                                                               List<Requests> timeOff) {
        List<Shifts> newShifts = new ArrayList<>();
        try {
            Instant startDate = batch.getStartDate();
            while (startDate.isBefore(batch.getEndDate())) {
                // Check if today is a holiday and create shifts if it isn't
                Instant finalStartDate = startDate;
                boolean isHoliday = holidays.stream().anyMatch(tempHoliday -> {
                    String[] splitDate = finalStartDate.atZone(ZoneId.of(timezone)).toString().split("T")[0].split("/");
                    return tempHoliday.getYear() == Integer.parseInt(splitDate[0]) && tempHoliday.getMonth() ==
                            Integer.parseInt(splitDate[1]) && tempHoliday.getDate() == Integer.parseInt(splitDate[2]);
                });
                if (!isHoliday) {
                    // Create a list of new shifts for this day
                    List<Shifts> newShiftsForOneDay = createShiftsForOneDayOfBatch(startDate, existingShifts, preferences,
                            employeePreferenceWithUsers, timezone, batch.getCreatedBy(), employeeWithoutPreferences,
                            batch.isReleased(), batch.getId(), timeOff);
                    newShifts.addAll(newShiftsForOneDay);
                } else {
                    // TODO add notification for holiday
                    log.info("Shifts for date {} are being skipped as there is a holiday on that date", startDate);
                }
                startDate = startDate.plus(1, ChronoUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("Exception while saving shifts for batch: {}", batch.getId(), e);
        }
        return newShifts;
    }

    /**
     * For any employee for whom preferences are not set, a notification will be sent to the admin.
     *
     * @param date                        date for which shifts are being created
     * @param existingShifts              list of existing shifts
     * @param preferences                 all shift preferences for this location
     * @param employeePreferenceWithUsers all employees list with preferences
     * @param timezone                    timezone for this location
     * @return List of new shifts created for this single date
     */
    private static List<Shifts> createShiftsForOneDayOfBatch(Instant date, List<Shifts> existingShifts, List<ShiftSchedulingPreferences> preferences,
                                                             List<EmployeePreferencesWithUser> employeePreferenceWithUsers, String timezone,
                                                             String adminId, List<String> employeeWithoutPreferences, boolean instantRelease,
                                                             String batchId, List<Requests> timeOff) {
        List<Shifts> newShifts = new ArrayList<>();
        Map<String, List<ShiftSchedulingPreferences>> locationRolePreference = preferences.stream()
                .collect(Collectors.groupingBy(ShiftSchedulingPreferences::getLocationRoleId, Collectors.toList()));
        Map<String, List<Shifts>> shiftsToCheckPerLocationRole = getShiftsToCheckPerLocationRole(date, existingShifts,
                locationRolePreference);
        employeePreferenceWithUsers.parallelStream()
                .forEach(preference -> {
                    // id belongs to Employee preference table, if it is empty preferences are not set
                    if (StringUtils.isEmpty(preference.getId())) {
                        employeeWithoutPreferences.add(preference.getUserId());
                        log.info("User: {} found with no preferences.", preference.getUserId());
                    } else {
                        try {
                            Instant[] shiftDuration = ShiftSchedulingUtils.getStartEndFromEmployeePreference(date, preference, timezone);
                            boolean sameDayExistingShit = existingShifts.stream()
                                    .anyMatch(shift -> DateAndTimeUtils.isInstantSameDayAsAnotherInstant(date, shift.getStart()));
                            if (shiftDuration != null) {
                                Shifts newShift = new Shifts();
                                newShift.setBatchId(batchId);
                                newShift.setUserId(preference.getUserId());
                                newShift.setStart(shiftDuration[0]);
                                newShift.setEnd(shiftDuration[1]);
                                newShift.setShiftType(ShiftType.RECURRING);
                                newShift.setShiftStatus(instantRelease ? ShiftStatus.RELEASED : ShiftStatus.ASSIGNED);
                                newShift.setOrganizationId(preference.getOrganizationId());
                                newShift.setLocationId(preference.getLocationId());
                                newShift.setLocationRoleId(preference.getLocationRoleId());
                                newShift.setCreatedBy(adminId);
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
                                // TODO Send notifications for conflicts
                                if (conflicts.size() > 0) {
                                }
                                if (sameDayExistingShit) {
                                    newShift.setShiftStatus(ShiftStatus.FAILED);
                                    newShift.setNotes("An existing shift overlaps with this shift.");
                                    log.warn("A shift already exist on same day. " +
                                            "Skipping shift for user {}, on date {}", preference.getUserId(), date);
                                }
                                if (isTimeoff) {
                                    newShift.setShiftStatus(ShiftStatus.FAILED);
                                    newShift.setNotes("A time off request is already approved for this time.");
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
     * Mainly used in {@link ShiftSchedulingService} method getAllPreferencesForALocationOrUser
     *
     * @param locationRole location role info
     * @return Shift scheduling preferences for this role
     */
    public static ShiftSchedulingPreferences mappingFunction(LocationRole locationRole) {
        if (locationRole == null) return null;
        ShiftSchedulingPreferences preferences = new ShiftSchedulingPreferences();
        preferences.setLocationRoleId(locationRole.getId());
        preferences.setTotalHoursPerDayMin(locationRole.getTotalHoursPerDayMin());
        preferences.setTotalHoursPerDayMax(locationRole.getTotalHoursPerDayMax());
        preferences.setTotalHoursPerDayMin(locationRole.getTotalHoursPerDayMin());
        preferences.setMaxConsecutiveWorkDays(locationRole.getMaxConsecutiveWorkDays());
        return preferences;
    }

    /**
     * Mainly used in {@link ShiftSchedulingService} method getAllPreferencesForALocationOrUser
     *
     * @param locationRole location role info
     * @param userId       userId for which preference is fetched
     * @return Shift scheduling preferences for this role
     */
    public static ShiftSchedulingPreferences mappingFunction(LocationRole locationRole, String userId) {
        if (locationRole == null) return null;
        ShiftSchedulingPreferences preferences = mappingFunction(locationRole);
        preferences.setUserId(userId);
        return preferences;
    }

}