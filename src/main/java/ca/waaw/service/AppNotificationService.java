package ca.waaw.service;

import ca.waaw.domain.Notification;
import ca.waaw.domain.user.User;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.appnotifications.MailDto;
import ca.waaw.dto.appnotifications.MultipleNotificationDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.dto.appnotifications.InviteAcceptedMailDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.mapper.NotificationMapper;
import ca.waaw.repository.NotificationRepository;
import ca.waaw.service.email.javamailsender.TempMailService;
import ca.waaw.web.rest.utils.MessageConstants;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AppNotificationService {

    private final NotificationMailService notificationMailService;

    private final NotificationRepository notificationRepository;

    private final WebSocketService webSocketService;

    private final TempMailService tempMailService;

    private final MessageSource messageSource;

    public void sendMultipleApplicationNotification(List<MultipleNotificationDto> notifications) {
        List<Map<String, Object>> newNotifications = new ArrayList<>();
        notifications.forEach(notificationInfo -> {
            Locale locale = Locale.forLanguageTag(notificationInfo.getNotificationInfo().getLanguage());
            String title = messageSource.getMessage(notificationInfo.getMessageConstant()[0], null, locale);
            String description = messageSource.getMessage(notificationInfo.getMessageConstant()[1],
                    notificationInfo.getMessageArguments(), locale);
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setDescription(description);
            notification.setType(notificationInfo.getNotificationInfo().getType());
            notification.setUserId(notificationInfo.getNotificationInfo().getReceiverUuid());
            Map<String, Object> notificationAndUser = new HashMap<>();
            notificationAndUser.put("notification", notification);
            notificationAndUser.put("user", notificationInfo.getNotificationInfo().getReceiverUsername());
            newNotifications.add(notificationAndUser);
        });
        notificationRepository.saveAll(newNotifications.stream()
                .map(notification -> (Notification) notification.get("notification")).collect(Collectors.toList()));
        newNotifications.forEach(notification -> webSocketService.notifyUser(NotificationMapper
                        .entityToDto((Notification) notification.get("notification"), "UTC"),
                notification.get("user").toString()));
    }

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
            message.setAdminName(admin.getFullName());
            message.setEmail(admin.getEmail());
            message.setLangKey(user.getLangKey());
            message.setName(user.getFullName());
            message.setUserEmail(user.getEmail());
            notificationMailService.sendNewUserMailToAdmin(message, user.getOrganization().getName(), loginUrl);
        }
        // Internal Notification
        Notification notification = new Notification();
        notification.setUserId(admin.getId());
        notification.setType(NotificationType.EMPLOYEE);
        notification.setTitle(messageSource.getMessage(MessageConstants.inviteAccepted[0], null, new Locale(admin.getLangKey())));
        String description = messageSource.getMessage(MessageConstants.inviteAccepted[1], new String[]{user.getFullName(), user.getEmail(),
                user.getOrganization().getName(), message.getRole(), message.getLocation()}, new Locale(admin.getLangKey()));
        notification.setDescription(description);
        notificationRepository.save(notification);
        webSocketService.notifyUser(NotificationMapper.entityToDto(notification, user.getOrganization().getTimezone()), admin.getUsername());
    }

}
