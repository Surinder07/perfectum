package ca.waaw.web.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "Notification", description = "All Notification APIs")
public class NotificationController {

    @GetMapping("/v1/notifications/getAll")
    public ResponseEntity<String> getAllNotifications() {
        return null;
    }

    @PutMapping("/v1/notifications/markAsRead")
    public ResponseEntity<String> markNotificationAsRead(@RequestParam String id) {
        return null;
    }

    @PutMapping("/v1/notifications/markAllAsRead")
    public ResponseEntity<String> markAllNotificationsAsRead() {
        return null;
    }

}
