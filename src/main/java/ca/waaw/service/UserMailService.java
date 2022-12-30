package ca.waaw.service;

import ca.waaw.domain.User;
import ca.waaw.dto.MailDto;
import ca.waaw.dto.emailmessagedtos.InviteUserMailDto;
import ca.waaw.service.email.javamailsender.MailService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserMailService {

    private final static Logger log = LogManager.getLogger(UserMailService.class);

    private final MailService mailService;

    public void sendVerificationEmail(User userMessage, String activationLink) {
        MailDto messageDto = MailDto.builder()
                .name("User")
                .email(userMessage.getEmail())
                .actionUrl(activationLink)
                .langKey(userMessage.getLangKey())
                .message(userMessage)
                .build();
        log.debug("Sending verification email to '{}'", userMessage.getEmail());
        mailService.sendEmailFromTemplate(messageDto, "mail/verifyEmail", "email.verification.title");
    }

    public void sendInvitationEmail(List<InviteUserMailDto> mailDtoList, String organizationName) {
        List<MailDto> messageDtoList = mailDtoList.stream()
                        .map(mailDto -> MailDto.builder()
                                .email(mailDto.getUser().getEmail())
                                .actionUrl(mailDto.getInviteUrl())
                                .langKey(mailDto.getUser().getLangKey())
                                .message(mailDto.getUser())
                                .organizationName(organizationName)
                                .build()
                        ).collect(Collectors.toList());
        log.debug("Sending invitation email to '{}'", mailDtoList.stream()
                .map(dto -> dto.getUser().getEmail()).collect(Collectors.toList()));
        messageDtoList.forEach(messageDto -> mailService.sendEmailFromTemplate(messageDto, "mail/invitationEmail",
                "email.invitation.title", organizationName));
    }

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
