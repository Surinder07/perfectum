package ca.waaw.dto.appnotifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipleNotificationDto {

    private String[] messageConstant;

    private String[] messageArguments;

    private NotificationInfoDto notificationInfo;

}
