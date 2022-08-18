package ca.waaw.service;

import ca.waaw.domain.Notification;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.emailmessagedtos.InviteAcceptedMessageDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.NotificationType;
import ca.waaw.repository.NotificationRepository;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class NotificationInternalService {

    private final NotificationMailService notificationMailService;

    private final NotificationRepository notificationRepository;

    /**
     * Will send an email notification to admin if email notifications are will on and send an application notification
     * to admin
     *
     * @param user     User that registered through invite
     * @param admin    Admin that sent the invite
     * @param loginUrl Login url for the application
     */
    public void notifyAdminAboutNewUser(UserOrganization user, User admin, String loginUrl) {
        InviteAcceptedMessageDto message = new InviteAcceptedMessageDto();
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
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(admin.getId());
        notification.setType(NotificationType.USER);
        notification.setTitle(CommonUtils.getPropertyFromMessagesResourceBundle("notification.invite.accepted.title", null));
        String description = String.format(CommonUtils.getPropertyFromMessagesResourceBundle("notification.invite.accepted.content", null),
                CommonUtils.combineFirstAndLastName(user.getFirstName(), user.getLastName()), user.getEmail(),
                user.getOrganization().getName(), message.getRole(), message.getLocation());
        notification.setDescription(description);
        notificationRepository.save(notification);
    }

    /**
     * Will update location_role and location information in message based on user roles
     *
     * @param message {@link InviteAcceptedMessageDto} object
     * @param user    invited user details
     */
    private void populateInviteAcceptedMessageLocationAndRole(InviteAcceptedMessageDto message, UserOrganization user) {
        String role;
        String location;
        if (user.getAuthority().equals(Authority.ADMIN)) {
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
