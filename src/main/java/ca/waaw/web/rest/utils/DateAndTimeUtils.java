package ca.waaw.web.rest.utils;

import ca.waaw.dto.DateTimeDto;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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
     * @param date     Date in format yyyy/MM/dd
     * @param time     Time in format HH:mm
     * @param timezone timezone to which we will change time to
     * @return Instant object for the date in given timezone
     */
    public static Instant getDateInstant(String date, String time, String timezone) {
        DateTimeFormatter formatter = time.length() == 5 ? DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm") :
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(String.format("%s %s", date, time), formatter);
        return dateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

    /**
     * @param timezone timezone, current date required in
     * @return Instant object for current date and time
     */
    public static Instant getCurrentDateTime(String timezone) {
        return ZonedDateTime.now(ZoneId.of(timezone)).toInstant();
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
     * @param date     date in string format(yyyy/MM/dd)
     * @param timezone timezone required
     * @return start and end time
     */
    public static Instant[] getStartAndEndTimeForInstant(String date, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        Instant start = LocalDateTime.parse(String.format("%s %s", date, "00:00:00"), formatter)
                .atZone(ZoneId.of(timezone)).toInstant();
        Instant end = LocalDateTime.parse(String.format("%s %s", date, "23:59:59"), formatter)
                .atZone(ZoneId.of(timezone)).toInstant();
        return new Instant[]{start, end};
    }

    /**
     * @param startDate start date in string format(yyyy/MM/dd)
     * @param endDate   end date in string format(yyyy/MM/dd)
     * @param timezone  timezone required
     * @return start and end time
     */
    public static Instant[] getStartAndEndTimeForInstant(String startDate, String endDate, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
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
     * @param date      date to be converted (Format: yyyy/MM/dd)
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(String.format("%s %s", date, time), formatter);
        return dateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

}
