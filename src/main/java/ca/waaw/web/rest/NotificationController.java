package ca.waaw.web.rest;

import ca.waaw.dto.NotificationDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.web.rest.service.NotificationService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerOk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "Notifications", description = "All Notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get all notifications, Page numbers start with 0")
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = NotificationDto.class)))},
            description = " Response will contain, total number of pages(totalPages), number of entries(totalEntries) and the list response(data)")
    @GetMapping("/v1/notifications/getAll")
    public ResponseEntity<PaginationDto> getAllNotifications(@PathVariable int pageNo, @PathVariable int pageSize) {
        return ResponseEntity.ok(notificationService.getAllNotifications(pageNo, pageSize));
    }

    @Operation(summary = "Mark notification with given id as read")
    @SwaggerAuthenticated
    @SwaggerBadRequest
    @SwaggerOk
    @PutMapping("/v1/notifications/markAsRead")
    public void markNotificationAsRead(@RequestParam String id) {
        notificationService.markNotificationAsRead(id);
    }

    @Operation(summary = "Mark all notifications as read")
    @SwaggerAuthenticated
    @SwaggerOk
    @PutMapping("/v1/notifications/markAllAsRead")
    public void markAllNotificationsAsRead() {
        notificationService.markAllNotificationAsRead();
    }

    @Operation(summary = "Delete a notification")
    @SwaggerAuthenticated
    @SwaggerBadRequest
    @SwaggerOk
    @DeleteMapping("/v1/notifications/delete")
    public void deleteNotification(@RequestParam String id) {
        notificationService.deleteNotification(id);
    }

}
