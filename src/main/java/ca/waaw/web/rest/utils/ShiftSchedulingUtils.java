package ca.waaw.web.rest.utils;

import ca.waaw.domain.Shifts;
import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.web.rest.errors.exceptions.application.ShiftOverlappingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ShiftSchedulingUtils {

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
        long shiftSecondsForSameDay = checkShiftOverlapAndReturnShiftHours(existingSameDatShifts,
                shift.getStart(), shift.getEnd());
        // Check for maximum and minimum hours per day of shift
        if ((shiftDurationInSeconds + shiftSecondsForSameDay) > shiftSchedulingPreferences.getTotalHoursPerDayMax() * 3600L) {
            conflictReasons.add("Maximum hours for shift per day exceeded");
        } else if ((shiftDurationInSeconds + shiftSecondsForSameDay) < shiftSchedulingPreferences.getTotalHoursPerDayMin() * 3600L) {
            conflictReasons.add("Minimum hours for shift per day not reached.");
        }
        // Check for Consecutive Days
        validateMaximumConsecutiveWorkDays(shiftsToCheck, shift, conflictReasons, shiftSchedulingPreferences);
        // Check for minimum gap in two shifts
        validateGapBetweenTwoShifts(shiftsToCheck, shift, conflictReasons, shiftSchedulingPreferences);
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
            if (conflict.isTrue()) conflictReasons.add("Maximum consecutive days are not being followed.");
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
        List<Float> allShiftDurations = Stream.of(dto.getMondayWorkingHours(), dto.getTuesdayWorkingHours(), dto.getWednesdayWorkingHours(),
                        dto.getThursdayWorkingHours(), dto.getFridayWorkingHours(), dto.getSaturdayWorkingHours(), dto.getSundayWorkingHours())
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
        return checkTimeDifferenceForTwoDays(dto.getMondayStartTime(), dto.getMondayWorkingHours(), dto.getTuesdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getTuesdayStartTime(), dto.getTuesdayWorkingHours(), dto.getWednesdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getWednesdayStartTime(), dto.getWednesdayWorkingHours(), dto.getThursdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getThursdayStartTime(), dto.getThursdayWorkingHours(), dto.getFridayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getFridayStartTime(), dto.getFridayWorkingHours(), dto.getSaturdayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getSaturdayStartTime(), dto.getSaturdayWorkingHours(), dto.getSundayStartTime(), minHourDifference)
                || checkTimeDifferenceForTwoDays(dto.getSundayStartTime(), dto.getSundayWorkingHours(), dto.getMondayStartTime(), minHourDifference);
    }

    /**
     * @param firstDayStart       first day start time (24:00 hours pattern)
     * @param firstDayWorkingHour working hours for first day
     * @param secondDayStart      second day start time (24:00 hours pattern)
     * @param minHourDifference   min difference between two shifts required
     * @return true if min difference between two shifts is not followed
     */
    private static boolean checkTimeDifferenceForTwoDays(String firstDayStart, float firstDayWorkingHour, String secondDayStart,
                                                         int minHourDifference) {
        if (StringUtils.isNotEmpty(firstDayStart) && StringUtils.isNotEmpty(secondDayStart)) {
            return DateAndTimeUtils.getTimeDifference(firstDayStart, secondDayStart, 1) <
                    (minHourDifference + firstDayWorkingHour);
        }
        return false;
    }

}