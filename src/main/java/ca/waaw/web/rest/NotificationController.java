package ca.waaw.web.rest;

import ca.waaw.dto.NotificationDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.web.rest.service.NotificationService;
import ca.waaw.web.rest.utils.APIConstants;
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
@Tag(name = APIConstants.TagNames.notification, description = APIConstants.TagDescription.notification)
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = APIConstants.ApiDescription.Notification.getAllNotification)
    @SwaggerAuthenticated
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = NotificationDto.class)))},
            description = APIConstants.SchemaDescription.pagination)
    @GetMapping(APIConstants.ApiEndpoints.Notification.getAllNotification)
    public ResponseEntity<PaginationDto> getAllNotifications(@PathVariable int pageNo, @PathVariable int pageSize) {
        return ResponseEntity.ok(notificationService.getAllNotifications(pageNo, pageSize));
    }

    @Operation(summary = APIConstants.ApiDescription.Notification.markNotificationAsRead)
    @SwaggerAuthenticated
    @SwaggerBadRequest
    @SwaggerOk
    @PutMapping(APIConstants.ApiEndpoints.Notification.markNotificationAsRead)
    public void markNotificationAsRead(@RequestParam String id) {
        notificationService.markNotificationAsRead(id);
    }

    @Operation(summary = APIConstants.ApiDescription.Notification.markAllNotificationAsRead)
    @SwaggerAuthenticated
    @SwaggerOk
    @PutMapping(APIConstants.ApiEndpoints.Notification.markAllNotificationAsRead)
    public void markAllNotificationsAsRead() {
        notificationService.markAllNotificationAsRead();
    }

    @Operation(summary = APIConstants.ApiDescription.Notification.deleteNotification)
    @SwaggerAuthenticated
    @SwaggerBadRequest
    @SwaggerOk
    @DeleteMapping(APIConstants.ApiEndpoints.Notification.deleteNotification)
    public void deleteNotification(@RequestParam String id) {
        notificationService.deleteNotification(id);
    }

}
