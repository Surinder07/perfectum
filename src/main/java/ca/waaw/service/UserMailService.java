package ca.waaw.service;

import ca.waaw.domain.User;
import ca.waaw.dto.MailDto;
import ca.waaw.service.email.javamailsender.MailService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserMailService {

    private final static Logger log = LogManager.getLogger(UserMailService.class);

    private final MailService mailService;

    @Async
    public void sendActivationEmail(User userMessage, String activationLink) {
        MailDto messageDto = MailDto.builder()
                .email(userMessage.getEmail())
                .actionUrl(activationLink)
                .langKey(userMessage.getLangKey())
                .message(userMessage)
                .build();
        log.debug("Sending activation email to '{}'", userMessage.getEmail());
        mailService.sendEmailFromTemplate(messageDto, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendInvitationEmail(User userMessage, String inviteLink, String organizationName) {
        MailDto messageDto = MailDto.builder()
                .email(userMessage.getEmail())
                .actionUrl(inviteLink)
                .langKey("en")
                .message(userMessage)
                .organizationName(organizationName)
                .build();
        log.debug("Sending invitation email to '{}'", userMessage.getEmail());
        mailService.sendEmailFromTemplate(messageDto, "mail/invitationEmail", "email.invitation.title",
                organizationName);
    }

    @Async
    public void sendPasswordResetMail(User userMessage, String resetLink) {
        MailDto messageDto = MailDto.builder()
                .email(userMessage.getEmail())
                .actionUrl(resetLink)
                .langKey(userMessage.getLangKey())
                .message(userMessage)
                .build();
        log.debug("Sending password reset email to '{}'", userMessage.getEmail());
        mailService.sendEmailFromTemplate(messageDto, "mail/passwordResetEmail", "email.reset.title");
    }

}
