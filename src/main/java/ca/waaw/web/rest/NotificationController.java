package ca.waaw.web.rest;

import ca.waaw.dto.NotificationDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.web.rest.service.NotificationService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerNotFound;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerOk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.notification}")
public class NotificationController {

    private final NotificationService notificationService;

    @SwaggerAuthenticated
    @Operation(description = "${api.description.notification.getAllNotification}")
    @GetMapping("${api.endpoints.notification.getAllNotification}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = NotificationDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllNotifications(@PathVariable int pageNo, @PathVariable int pageSize) {
        return ResponseEntity.ok(notificationService.getAllNotifications(pageNo, pageSize));
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.notification.markNotificationAsRead}")
    @PutMapping("${api.endpoints.notification.markNotificationAsRead}")
    public void markNotificationAsRead(@RequestParam String id) {
        notificationService.markNotificationAsRead(id);
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.notification.markAllNotificationAsRead}")
    @PutMapping("${api.endpoints.notification.markAllNotificationAsRead}")
    public void markAllNotificationsAsRead() {
        notificationService.markAllNotificationAsRead();
    }

    @SwaggerOk
    @SwaggerNotFound
    @SwaggerBadRequest
    @SwaggerAuthenticated
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "${api.description.notification.deleteNotification}")
    @DeleteMapping("${api.endpoints.notification.deleteNotification}")
    public void deleteNotification(@RequestParam String id) {
        notificationService.deleteNotification(id);
    }

}
