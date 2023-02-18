package ca.waaw.dto;

import ca.waaw.enumration.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInfoDto {

    private String receiverUuid;

    private String receiverMail;

    private String receiverName;

    private String receiverMobile;

    private String language = "en";

    private NotificationType type;

}