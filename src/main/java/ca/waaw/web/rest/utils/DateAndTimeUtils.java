package ca.waaw.web.rest.utils;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.TimeDto;
import ca.waaw.enumration.DaysOfWeek;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class DateAndTimeUtils {

    /**
     * @param dateTime Instant object from the database (UTC)
     * @param timezone timezone to which we will change time to
     * @return Date Time object with date and time to be sent to frontend
     */
    public static DateTimeDto getDateTimeObject(Instant dateTime, String timezone) {
        String[] splitDate = dateTime.atZone(ZoneId.of(timezone)).toString().split("T");
        String time = splitDate[1].substring(0, 5);
        return DateTimeDto.builder().date(splitDate[0]).time(time).build();
    }

    /**
     * @param dateTime Instant object from the database (UTC)
     * @param timezone timezone to which we will change time to
     * @return Date Time object with date and time to be sent to frontend with date like(Jun 22, 2022)
     */
    public static DateTimeDto getDateTimeObjectWithFullDate(Instant dateTime, String timezone) {
        String[] splitDate = dateTime.atZone(ZoneId.of(timezone)).toString().split("T");
        String time = splitDate[1].substring(0, 5);
        return DateTimeDto.builder().date(getFullMonthDate(dateTime, timezone)).time(time).build();
    }

    /**
     * @param date     Date in format yyyy-MM-dd
     * @param time     Time in format HH:mm
     * @param timezone timezone to which we will change time to
     * @return Instant object for the date in given timezone
     */
    public static Instant getDateInstant(String date, String time, String timezone) {
        DateTimeFormatter formatter = time.length() == 5 ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") :
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(String.format("%s %s", date, time), formatter);
        return dateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

    /**
     * @param date     date to be converted to required format
     * @param timezone timezone for which date is required
     * @return date in format dd month name, yyyy as a String
     */
    public static String getDateWithFullMonth(Instant date, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM, yyyy");
        return formatter.format(date.atZone(ZoneId.of(timezone)));
    }

    /**
     * @param timezone timezone, current date required in
     * @return DateTime object for current date and time
     */
    public static DateTimeDto getCurrentDateTime(String timezone) {
        String dateString = Instant.now().atZone(ZoneId.of(timezone)).toString();
        String[] date = dateString.split("T");
        String[] timeString = date[1].substring(0, 5).split(":");
        return DateTimeDto.builder()
                .date(date[0])
                .time(timeString[0] + ":" + timeString[1])
                .build();
    }

    /**
     * This method is used to check if user is attempting to update holiday in the past, so we are adding 12
     * hours (negative) to have flexibility for all timezones if timezone is not passed in method.
     *
     * @param type     year, month or date
     * @param timezone timezone for which date is needed
     * @return integer value for year month or date
     */
    public static int getCurrentDate(String type, String timezone) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        switch (type.toLowerCase()) {
            case "year":
                return now.getYear();
            case "month":
                return now.getMonthValue();
            case "date":
                return now.getDayOfMonth();
        }
        return 0;
    }

    /**
     * @param time1         first time for past date
     * @param time2         second time for future date
     * @param dayDifference difference of days between two times
     * @return difference in times in hours
     */
    public static float getTimeDifference(String time1, String time2, int dayDifference) {
        Instant day1 = Instant.now().atZone(ZoneOffset.UTC)
                .withHour(Integer.parseInt(time1.split(":")[0]))
                .withMinute(Integer.parseInt(time1.split(":")[1]))
                .withSecond(0).withNano(0).toInstant();
        Instant day2 = Instant.now().atZone(ZoneOffset.UTC)
                .withHour(Integer.parseInt(time2.split(":")[0]))
                .withMinute(Integer.parseInt(time2.split(":")[1]))
                .withSecond(0).withNano(0).toInstant().plus(dayDifference, ChronoUnit.DAYS);
        float differenceInSeconds = day2.getEpochSecond() - day1.getEpochSecond();
        return (differenceInSeconds / 3600);
    }

    public static long getSameDayTimeDifference(String time1, String time2) {
        Instant day1 = Instant.now().atZone(ZoneOffset.UTC)
                .withHour(Integer.parseInt(time1.split(":")[0]))
                .withMinute(Integer.parseInt(time1.split(":")[1]))
                .withSecond(0).withNano(0).toInstant();
        Instant day2 = Instant.now().atZone(ZoneOffset.UTC)
                .withHour(Integer.parseInt(time2.split(":")[0]))
                .withMinute(Integer.parseInt(time2.split(":")[1]))
                .withSecond(0).withNano(0).toInstant();
        float differenceInSeconds = day2.getEpochSecond() - day1.getEpochSecond();
        return (long) (differenceInSeconds / 60);
    }

    /**
     * Example: For Instant 2022-09-21T14:53:55 it will return [2022-09-21T00:00:00, 2022-09-21T23:59:59]
     *
     * @param date date for which start and end time are needed
     * @return An array of Instants with start and end for the date
     */
    public static Instant[] getStartAndEndTimeForInstant(Instant date) {
        Instant start = date.atZone(ZoneOffset.UTC)
                .withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
        Instant end = date.atZone(ZoneOffset.UTC)
                .withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant();
        return new Instant[]{start, end};
    }

    /**
     * @param date     date in string format(yyyy-MM-dd)
     * @param timezone timezone required
     * @return start and end time
     */
    public static Instant[] getStartAndEndTimeForInstant(String date, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant start = LocalDateTime.parse(String.format("%s %s", date, "00:00:00"), formatter)
                .atZone(ZoneId.of(timezone)).toInstant();
        Instant end = LocalDateTime.parse(String.format("%s %s", date, "23:59:59"), formatter)
                .atZone(ZoneId.of(timezone)).toInstant();
        return new Instant[]{start, end};
    }

    /**
     * @param startDate start date in string format(yyyy-MM-dd)
     * @param endDate   end date in string format(yyyy-MM-dd)
     * @param timezone  timezone required
     * @return start and end time
     */
    public static Instant[] getStartAndEndTimeForInstant(String startDate, String endDate, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant start = LocalDateTime.parse(String.format("%s %s", startDate, "00:00:00"), formatter)
                .atZone(ZoneId.of(timezone)).toInstant();
        Instant end = LocalDateTime.parse(String.format("%s %s", endDate, "23:59:59"), formatter)
                .atZone(ZoneId.of(timezone)).toInstant();
        return new Instant[]{start, end};
    }

    /**
     * Example: For Instant 2022-09-21T14:53:55 and day difference 2 it will return
     * [2022-09-21T00:00:00, 2022-09-23T23:59:59]
     *
     * @param date       date for which start and end time are needed
     * @param difference difference between start and end date
     * @return An array of Instants with start and end for the dates
     */
    public static Instant[] getStartAndEndTimeForInstant(Instant date, int difference) {
        Instant start = date.atZone(ZoneOffset.UTC)
                .withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
        Instant end = date.atZone(ZoneOffset.UTC)
                .withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant()
                .plus(difference, ChronoUnit.DAYS);
        return new Instant[]{start, end};
    }

    public static Instant[] getStartAndEndTimeForInstant(Instant date, int difference, String timezone) {
        Instant start = date.atZone(ZoneId.of(timezone))
                .withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
        Instant end = date.atZone(ZoneId.of(timezone))
                .withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant()
                .plus(difference, ChronoUnit.DAYS);
        return new Instant[]{start, end};
    }

    /**
     * @param date       date to be checked
     * @param startLimit start date for range
     * @param endLimit   end date for range
     * @return true if date is between start and end
     */
    public static boolean isInstantBetweenInstants(Instant date, Instant startLimit, Instant endLimit) {
        return (date.isAfter(startLimit) || date.equals(startLimit)) &&
                (date.isBefore(endLimit) || date.equals(endLimit));
    }

    /**
     * @param date      date to be checked
     * @param reference date to be checked against
     * @return true if date falls on the same day as reference
     */
    public static boolean isInstantSameDayAsAnotherInstant(Instant date, Instant reference) {
        Instant[] startAndEndForReferenceDate = getStartAndEndTimeForInstant(reference);
        return isInstantBetweenInstants(date, startAndEndForReferenceDate[0], startAndEndForReferenceDate[1]);
    }

    /**
     * @param date      date to be converted (Format: yyyy-MM-dd)
     * @param timeOfDay start/end time at which Instant is required
     * @return Instant for given date and time
     * @throws Exception if type is invalid
     */
    public static Instant getDateAtStartOrEnd(String date, String timeOfDay, String timezone) throws Exception {
        String time;
        if (timeOfDay.equalsIgnoreCase("start")) {
            time = "00:00:00";
        } else if (timeOfDay.equalsIgnoreCase("end")) {
            time = "23:59:59";
        } else throw new Exception("Invalid timeOfDay");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(String.format("%s %s", date, time), formatter);
        return dateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

    /**
     * @param timezone timezone for which dates are required
     * @param start    start of the week
     * @return start and end for the current week
     */
    public static Instant[] getCurrentWeekStartEnd(String timezone, DaysOfWeek start) {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of(timezone));
        Instant weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(start.toString())))
                .withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
        Instant weekEnd = weekStart.plus(7, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS);
        return new Instant[]{weekStart, weekEnd};
    }

    /**
     * @param timezone timezone for which dates are required
     * @return start and end for the today
     */
    public static Instant[] getTodayInstantRange(String timezone) {
        String today = LocalDateTime.now().atZone(ZoneId.of(timezone)).toString().split("T")[0];
        return getStartAndEndTimeForInstant(today, timezone);
    }

    /**
     * @param year     year
     * @param month    month
     * @param date     date
     * @param timezone timezone
     * @return Instant object with above details
     */
    public static Instant toDate(int year, int month, int date, String timezone) {
        return Date.from(LocalDateTime.of(year, month, date, 0, 0, 0, 0)
                .atZone(ZoneId.of(timezone)).toInstant()).toInstant();
    }

    /**
     * @param year     year
     * @param month    month
     * @param timezone timezone
     * @return Instant range for the given month
     */
    public static Instant[] getMonthStartEnd(int year, int month, String timezone) {
        Instant dateStart = Date.from(LocalDateTime.of(year, month, 1, 0, 0, 0, 0)
                .atZone(ZoneId.of(timezone)).toInstant()).toInstant();

        Instant dateEnd = Date.from(LocalDateTime.of(year, month, 1, 0, 0, 0, 0)
                .atZone(ZoneId.of(timezone)).plusMonths(1).minus(1, ChronoUnit.MILLIS).toInstant()).toInstant();
        return new Instant[]{dateStart, dateEnd};
    }

    /**
     * @param date     Instant object
     * @param timezone timezone
     * @return date in format (Jan 13, 2023)
     */
    public static String getFullMonthDate(Instant date, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("MMM dd, yyyy")
                .withZone(ZoneId.of(timezone));
        return LocalDateTime
                .ofInstant(date, ZoneId.of(timezone))
                .format(formatter).replace(".", "");
    }

    /**
     * @param date     Instant object
     * @param timezone timezone
     * @return date in format (Jan 13, 2023 21:00)
     */
    public static String getFullMonthDateWithTime(Instant date, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("MMM dd, yyyy")
                .withZone(ZoneId.of(timezone));
        String newDate = LocalDateTime
                .ofInstant(date, ZoneId.of(timezone))
                .format(formatter).replace(".", "");
        String time = getDateTimeObject(date, timezone).getTime();
        return newDate + " " + time;
    }

    /**
     * @param start start Instant
     * @param end   end Instant
     * @return time difference between instants in format hh:mm:ss
     */
    public static String getTimeBetweenInstants(Instant start, Instant end) {
        int hour = (int) Math.abs(ChronoUnit.HOURS.between(start, end));
        int minute = (int) Math.abs(ChronoUnit.MINUTES.between(start, end));
        int second = (int) Math.abs(ChronoUnit.SECONDS.between(start, end));
        return String.format("%s:%s:%s", StringUtils.leftPad(String.valueOf(hour), 2, "0"),
                StringUtils.leftPad(String.valueOf(minute % 60), 2, "0"),
                StringUtils.leftPad(String.valueOf(second % 60), 2, "0"));
    }

    public static String getInstantAsStringInGivenTimezone(Instant date, String timezone) {
        DateTimeDto dto = getDateTimeObject(date, timezone);
        return String.format("%sT%s:00", dto.getDate(), dto.getTime());
    }

    public static String getDateTimeAsString(Instant date, String timezone) {
        Instant[] todayRange = getTodayInstantRange(timezone);
        Instant[] yesterdayRange = new Instant[]{todayRange[0].minus(1, ChronoUnit.DAYS),
                todayRange[1].minus(1, ChronoUnit.DAYS)};
        DateTimeDto dateTime = getDateTimeObjectWithFullDate(date, timezone);
        String newDate = null;
        if (!date.isBefore(todayRange[0]) && date.isBefore(todayRange[1])) newDate = "today";
        if (!date.isBefore(yesterdayRange[0]) && date.isBefore(yesterdayRange[1])) newDate = "yesterday";
        return newDate == null ? dateTime.getDate() : newDate + " " + dateTime.getTime();
    }

    public static int getTotalMinutesForTime(TimeDto dto) {
        return (dto.getHours() * 60) + dto.getMinutes();
    }

    public static TimeDto getHourMinuteTimeFromMinutes(long minutes) {
        return TimeDto.builder()
                .hours((int) (minutes / 60))
                .minutes((int) (minutes % 60))
                .build();
    }

    public static boolean isSameDay(Instant date1, Instant date2, String timezone) {
        return getDateTimeObject(date1, timezone).getDate().equals(getDateTimeObject(date2, timezone).getDate());
    }

    /**
     * @param date     date to check year for (Format: yyyy-MM-dd)
     * @param timezone timezone for user
     * @return checks if the date is same year as today's date
     */
    public static boolean isSameYear(String date, String timezone) {
        String today = LocalDateTime.now().atZone(ZoneId.of(timezone)).toString().split("T")[0];
        return today.split("-")[0].equals(date.split("-")[0]);
    }

}
