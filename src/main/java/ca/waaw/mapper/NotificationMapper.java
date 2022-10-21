package ca.waaw.mapper;

import ca.waaw.domain.Notification;
import ca.waaw.dto.NotificationDto;
import org.springframework.beans.BeanUtils;

public class NotificationMapper {

    /**
     * @param source notification entity
     * @return dto with mapped details from entity
     */
    public static NotificationDto entityToDto(Notification source) {
        NotificationDto target = new NotificationDto();
        BeanUtils.copyProperties(source, target);
        target.setType(source.getType().toString());
        return target;
    }

}
