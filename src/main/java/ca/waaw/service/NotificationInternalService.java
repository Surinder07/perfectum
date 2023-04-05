package ca.waaw.service;

import ca.waaw.domain.Notification;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.NotificationInfoDto;
import ca.waaw.dto.emailmessagedtos.InviteAcceptedMailDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.NotificationType;
import ca.waaw.mapper.NotificationMapper;
import ca.waaw.repository.NotificationRepository;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@AllArgsConstructor
public class NotificationInternalService {

    private final NotificationMailService notificationMailService;

    private final NotificationRepository notificationRepository;

    private final WebSocketService webSocketService;

    public void sendNotification(String propertyKey, NotificationInfoDto notificationInfo, String... messageArguments) {
        Map<String, String> properties = CommonUtils.getPropertyMapFromMessagesResourceBundle(propertyKey,
                notificationInfo.getLanguage());
        String message = String.format(properties.get("notification"), messageArguments);
        Notification notification = new Notification();
        notification.setTitle(properties.get("title"));
        notification.setDescription(message);
        notification.setType(notificationInfo.getType());
        notification.setUserId(notificationInfo.getReceiverUuid());
        notificationRepository.save(notification);
        webSocketService.notifyUser(NotificationMapper.entityToDto(notification, "UTC"),
                notificationInfo.getReceiverUsername());
    }

    /**
     * Will send an email notification to admin if email notifications are will on and send an application notification
     * to admin
     *
     * @param user     User that registered through invite
     * @param admin    Admin that sent the invite
     * @param loginUrl Login url for the application
     */
    public void notifyAdminAboutNewUser(UserOrganization user, User admin, String loginUrl) {
        InviteAcceptedMailDto message = new InviteAcceptedMailDto();
        populateInviteAcceptedMessageLocationAndRole(message, user);
        if (admin.isEmailNotifications()) {
            message.setAdminName(CommonUtils.combineFirstAndLastName(admin.getFirstName(), admin.getLastName()));
            message.setEmail(admin.getEmail());
            message.setLangKey(user.getLangKey());
            message.setName(CommonUtils.combineFirstAndLastName(user.getFirstName(), user.getLastName()));
            message.setUserEmail(user.getEmail());
            notificationMailService.sendNewUserMailToAdmin(message, user.getOrganization().getName(), loginUrl);
        }
        // Internal Notification
        Notification notification = new Notification();
        notification.setUserId(admin.getId());
        notification.setType(NotificationType.EMPLOYEE);
        notification.setTitle(CommonUtils.getPropertyFromMessagesResourceBundle("notification.invite.accepted.title", null));
        String description = String.format(CommonUtils.getPropertyFromMessagesResourceBundle("notification.invite.accepted.content", null),
                user.getFullName(), user.getEmail(),
                user.getOrganization().getName(), message.getRole(), message.getLocation());
        notification.setDescription(description);
        notificationRepository.save(notification);
        //todo change to admins timezone
        webSocketService.notifyUser(NotificationMapper.entityToDto(notification, user.getLocation().getTimezone()), admin.getUsername());
    }

    /**
     * Will update location_role and location information in message based on user roles
     *
     * @param message {@link InviteAcceptedMailDto} object
     * @param user    invited user details
     */
    private void populateInviteAcceptedMessageLocationAndRole(InviteAcceptedMailDto message, UserOrganization user) {
        String role;
        String location;
        if (user.getAuthority().equals(Authority.ADMIN) || user.getAuthority().equals(Authority.SUPER_USER )) {
            role = "Global Admin";
            location = "All";
        } else if (user.getAuthority().equals(Authority.MANAGER)) {
            location = user.getLocation().getName();
            role = "Location Admin";
        } else {
            location = user.getLocation().getName();
            role = String.format(user.getAuthority().equals(Authority.CONTRACTOR) ? "Contractor / %s" : "Employee / %s",
                    user.getLocationRole().getName());
        }
        message.setLocation(location);
        message.setUserEmail(user.getEmail());
        message.setRole(role);
    }

}
