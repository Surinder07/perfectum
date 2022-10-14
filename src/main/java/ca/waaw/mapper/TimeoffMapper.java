package ca.waaw.mapper;

import ca.waaw.domain.TimeOffs;
import ca.waaw.dto.NewTimeOffDto;
import ca.waaw.web.rest.utils.DateAndTimeUtils;

public class TimeoffMapper {

    /**
     *
     * @param source source dto
     * @param timezone location timezone of employee
     * @return Timeoff entity
     */
    public static TimeOffs dtoToEntity(NewTimeOffDto source, String timezone) {
        TimeOffs target = new TimeOffs();
        target.setStartDate(DateAndTimeUtils.getDateInstant(source.getStartDate().getDate(),
                source.getStartDate().getTime(), timezone));
        target.setStartDate(DateAndTimeUtils.getDateInstant(source.getEndDate().getDate(),
                source.getEndDate().getTime(), timezone));
        target.setNote(source.getNote());
        target.setUserId(source.getUserId());
        return target;
    }

}
