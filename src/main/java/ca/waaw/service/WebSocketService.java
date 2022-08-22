package ca.waaw.service;

import ca.waaw.config.applicationconfig.WebSocketConfig;
import ca.waaw.dto.NotificationDto;
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

}
