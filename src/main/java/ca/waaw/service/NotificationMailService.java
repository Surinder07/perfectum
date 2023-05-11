package ca.waaw.service;

import ca.waaw.dto.appnotifications.MailDto;
import ca.waaw.dto.appnotifications.InviteAcceptedMailDto;
import ca.waaw.service.email.javamailsender.MailService;
import ca.waaw.service.email.javamailsender.TempMailService;
import ca.waaw.web.rest.utils.MessageConstants;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NotificationMailService {

    private final static Logger log = LogManager.getLogger(NotificationMailService.class);

    private final MailService mailService;

    private final TempMailService tempMailService;

    public void sendNewUserMailToAdmin(InviteAcceptedMailDto message, String organizationName, String loginUrl) {
        MailDto messageDto = MailDto.builder()
                .name(message.getAdminName())
                .email(message.getEmail())
                .actionUrl(loginUrl)
                .langKey(message.getLangKey())
                .organizationName(organizationName)
                .buttonText("Start Scheduling")
                .message(message)
                .build();
        log.debug("Sending invite accepted notification email to '{}'", message.getEmail());
//        mailService.sendEmailFromTemplate(messageDto, "mail/inviteAccepted", "email.invite.accepted.title");
        tempMailService.sendEmailFromTemplate(messageDto, MessageConstants.emailInviteAccept, null, new String[]{organizationName, message.getName(),
                message.getUserEmail(), message.getLocation(), message.getRole()});
    }

}
