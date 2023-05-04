package ca.waaw.service;

import ca.waaw.domain.Notification;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.MailDto;
import ca.waaw.dto.NotificationInfoDto;
import ca.waaw.dto.emailmessagedtos.InviteAcceptedMailDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.mapper.NotificationMapper;
import ca.waaw.repository.NotificationRepository;
import ca.waaw.service.email.javamailsender.TempMailService;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@AllArgsConstructor
public class AppNotificationService {

    private final NotificationMailService notificationMailService;

    private final NotificationRepository notificationRepository;

    private final WebSocketService webSocketService;

    private final TempMailService tempMailService;

    private final MessageSource messageSource;

    public void sendApplicationNotification(String[] messageConstant, NotificationInfoDto notificationInfo, boolean sendEmail,
                                            String... messageArguments) {
        Locale locale = Locale.forLanguageTag(notificationInfo.getLanguage());
        String title = messageSource.getMessage(messageConstant[0], null, locale);
        String description = messageSource.getMessage(messageConstant[1], messageArguments, locale);
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setType(notificationInfo.getType());
        notification.setUserId(notificationInfo.getReceiverUuid());
        notificationRepository.save(notification);
        webSocketService.notifyUser(NotificationMapper.entityToDto(notification, "UTC"),
                notificationInfo.getReceiverUsername());
        if (sendEmail) {
            MailDto messageDto = MailDto.builder()
                    .email(notificationInfo.getReceiverMail())
                    .name(notificationInfo.getReceiverName())
                    .langKey(notificationInfo.getLanguage())
                    .build();
            tempMailService.sendEmailFromTemplate(messageDto, messageConstant, null, messageArguments);
        }
    }

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
        message.setLocation(user.getLocation().getName());
        message.setRole(user.getLocationRole().getName());
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

}
