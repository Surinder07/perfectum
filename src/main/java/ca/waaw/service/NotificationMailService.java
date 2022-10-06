package ca.waaw.service;

import ca.waaw.dto.MailDto;
import ca.waaw.dto.emailmessagedtos.InviteAcceptedMailDto;
import ca.waaw.service.email.javamailsender.MailService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NotificationMailService {

    private final static Logger log = LogManager.getLogger(NotificationMailService.class);

    private final MailService mailService;

    public void sendNewUserMailToAdmin(InviteAcceptedMailDto message, String organizationName, String loginUrl) {
        MailDto messageDto = MailDto.builder()
                .email(message.getEmail())
                .actionUrl(loginUrl)
                .langKey(message.getLangKey())
                .organizationName(organizationName)
                .message(message)
                .build();
        log.debug("Sending invite accepted notification email to '{}'", message.getEmail());
        mailService.sendEmailFromTemplate(messageDto, "mail/inviteAccepted", "email.invite.accepted.title");
    }

}
