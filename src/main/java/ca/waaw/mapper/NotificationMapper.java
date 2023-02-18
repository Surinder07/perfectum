package ca.waaw.mapper;

import ca.waaw.domain.Notification;
import ca.waaw.dto.NotificationDto;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.springframework.beans.BeanUtils;

public class NotificationMapper {

    /**
     * @param source notification entity
     * @return dto with mapped details from entity
     */
    public static NotificationDto entityToDto(Notification source, String timezone) {
        NotificationDto target = new NotificationDto();
        BeanUtils.copyProperties(source, target);
        target.setType(source.getType().toString());
        target.setCreatedTime(DateAndTimeUtils.getDateTimeAsString(source.getCreatedTime(), timezone));
        return target;
    }

}
