package ca.waaw.mapper;

import ca.waaw.domain.Notification;
import ca.waaw.dto.NotificationDto;
import org.springframework.beans.BeanUtils;

public class NotificationMapper {

    public static NotificationDto entityToDto(Notification source) {
        NotificationDto target = new NotificationDto();
        BeanUtils.copyProperties(source, target);
        target.setType(source.getType().toString());
        return target;
    }

}
