package ca.waaw.web.rest;

import ca.waaw.dto.NotificationDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.web.rest.service.NotificationService;
import ca.waaw.web.rest.utils.CommonUtils;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
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

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.notification.getAllNotification}")
    @GetMapping("${api.endpoints.notification.getAllNotification}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = NotificationDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllNotifications(@PathVariable int pageNo, @PathVariable int pageSize,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) String status) {
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            startDate = null;
            endDate = null;
        }
        if(StringUtils.isEmpty(type)) type = null;
        else CommonUtils.validateStringInEnum(NotificationType.class, type, "type");
        if(StringUtils.isEmpty(status)) status = null;
        Boolean isRead = status == null ? null : status.equalsIgnoreCase("read");
        return ResponseEntity.ok(notificationService.getAllNotifications(pageNo, pageSize, startDate, endDate,
                type, isRead));
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
