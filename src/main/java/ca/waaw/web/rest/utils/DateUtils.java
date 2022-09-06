package ca.waaw.web.rest.utils;

import ca.waaw.dto.DateTimeDto;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(String.format("%s %s", date, time), formatter);
        return dateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

}
