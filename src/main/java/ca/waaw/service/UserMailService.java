package ca.waaw.service;

import ca.waaw.domain.user.User;
import ca.waaw.dto.appnotifications.MailDto;
import ca.waaw.dto.appnotifications.InviteUserMailDto;
import ca.waaw.service.email.javamailsender.MailService;
import ca.waaw.service.email.javamailsender.TempMailService;
import ca.waaw.web.rest.utils.MessageConstants;
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

    private final TempMailService tempMailService;

    public void sendVerificationEmail(User userMessage, String activationLink) {
        MailDto messageDto = MailDto.builder()
                .name("User")
                .email(userMessage.getEmail())
                .actionUrl(activationLink)
                .buttonText("Verify Email")
                .langKey(userMessage.getLangKey())
                .message(userMessage)
                .build();
        log.debug("Sending verification email to '{}'", userMessage.getEmail());
        tempMailService.sendEmailFromTemplate(messageDto, MessageConstants.emailVerification, null, null);
//        mailService.sendEmailFromTemplate(messageDto, "mail/verifyEmail", "email.verification.title");
    }

    public void sendInvitationEmail(List<InviteUserMailDto> mailDtoList, String organizationName) {
        List<MailDto> messageDtoList = mailDtoList.stream()
                .map(mailDto -> MailDto.builder()
                        .email(mailDto.getUser().getEmail())
                        .name(mailDto.getUser().getFullName())
                        .actionUrl(mailDto.getInviteUrl())
                        .buttonText("Accept Invite")
                        .langKey(mailDto.getUser().getLangKey())
                        .message(mailDto.getUser())
                        .organizationName(organizationName)
                        .build()
                ).collect(Collectors.toList());
        log.debug("Sending invitation email to '{}'", mailDtoList.stream()
                .map(dto -> dto.getUser().getEmail()).collect(Collectors.toList()));
//        messageDtoList.forEach(messageDto -> mailService.sendEmailFromTemplate(messageDto, "mail/invitationEmail",
//                "email.invitation.title", organizationName));
        messageDtoList.forEach(messageDto -> tempMailService.sendEmailFromTemplate(messageDto, MessageConstants.emailInvitation,
                new String[]{organizationName}, new String[]{organizationName}));
    }

    public void sendPasswordResetMail(User userMessage, String resetLink) {
        MailDto messageDto = MailDto.builder()
                .email(userMessage.getEmail())
                .name(userMessage.getFullName())
                .actionUrl(resetLink)
                .langKey(userMessage.getLangKey())
                .buttonText("Reset Password")
                .message(userMessage)
                .build();
        log.debug("Sending password reset email to '{}'", userMessage.getEmail());
//        mailService.sendEmailFromTemplate(messageDto, "mail/passwordResetEmail", "email.reset.title");
        tempMailService.sendEmailFromTemplate(messageDto, MessageConstants.emailReset, null, null);
    }

    public void sendUpdateEmailMail(User user, String updateLink) {
        MailDto messageDto = MailDto.builder()
                .email(user.getEmailToUpdate())
                .name(user.getFirstName())
                .actionUrl(updateLink)
                .langKey(user.getLangKey())
                .buttonText("Verify Email")
                .build();
        tempMailService.sendEmailFromTemplate(messageDto, MessageConstants.emailUpdate, null, new String[]{user.getEmail(), user.getEmailToUpdate()});
//        mailService.sendEmailFromTemplate(mailDto, "mail/TitleDescriptionTemplate", "notification.email.update.title");
    }

}
