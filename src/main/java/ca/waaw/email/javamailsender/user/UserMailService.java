package ca.waaw.email.javamailsender.user;

import ca.waaw.domain.User;
import ca.waaw.dto.MailDto;
import ca.waaw.email.javamailsender.MailService;
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

}
