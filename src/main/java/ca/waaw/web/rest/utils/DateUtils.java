package ca.waaw.web.rest.utils;

import ca.waaw.dto.DateTimeDto;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static DateTimeDto changeTimezone(Instant dateTime, String timezone) {
        String[] splitDate = dateTime.atZone(ZoneId.of(timezone)).toString().split("T");
        String time = splitDate[1].substring(0,5);
        return DateTimeDto.builder().date(splitDate[0]).time(time).build();
    }

    public Instant getDateInstant(String date, String time, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(String.format("%s %s", date, time), formatter);
        return dateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

}
