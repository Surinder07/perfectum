package ca.waaw.service;

import ca.waaw.config.applicationconfig.WebSocketConfig;
import ca.waaw.dto.NotificationDto;
import ca.waaw.dto.userdtos.UserDetailsDto;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    private final WebSocketConfig webSocketConfig;

    public void notifyUser(NotificationDto message, String username) {
        messagingTemplate.convertAndSendToUser(username, webSocketConfig.getNotificationUrl(), message);
    }

    public void notifyUserAboutShiftCreation(String username) {
        messagingTemplate.convertAndSendToUser(username, webSocketConfig.getShiftUrl(), true);
    }

    public void notifyUserToAllowClockIn(String username, int clockInInfo) {
        messagingTemplate.convertAndSendToUser(username, webSocketConfig.getTimesheetUrl(), clockInInfo);
    }

    public void notifyUserAboutHolidayUploadComplete(String username) {
        messagingTemplate.convertAndSendToUser(username, webSocketConfig.getHolidayUploadUrl(), true);
    }

    public void notifyUserAboutInviteUploadComplete(String username) {
        messagingTemplate.convertAndSendToUser(username, webSocketConfig.getUserInviteUrl(), true);
    }

    public void updateUserDetailsForUi(String username, UserDetailsDto message) {
        messagingTemplate.convertAndSendToUser(username, webSocketConfig.getUpdateUserDetailUrl(), message);
    }

}
