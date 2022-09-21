package ca.waaw.web.rest.utils;

import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShiftSchedulingUtils {

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
    public static boolean checkTimeDifferenceForTwoDays(String firstDayStart, float firstDayWorkingHour, String secondDayStart,
                                                        int minHourDifference) {
        if (StringUtils.isNotEmpty(firstDayStart) && StringUtils.isNotEmpty(secondDayStart)) {
            return DateAndTimeUtils.getTimeDifference(firstDayStart, secondDayStart, 1) <
                    (minHourDifference + firstDayWorkingHour);
        }
        return false;
    }

}