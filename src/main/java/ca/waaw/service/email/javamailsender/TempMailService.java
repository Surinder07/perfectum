package ca.waaw.service.email.javamailsender;

import ca.waaw.config.applicationconfig.AppMailConfig;
import ca.waaw.dto.appnotifications.MailDto;
import ca.waaw.web.rest.utils.HtmlTemplates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@SuppressWarnings("unused")
@Service
public class TempMailService {

    private final Logger log = LogManager.getLogger(TempMailService.class);

    private final AppMailConfig appMailConfig;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    public TempMailService(AppMailConfig appMailConfig,
                           JavaMailSender javaMailSender,
                           MessageSource messageSource) {
        this.appMailConfig = appMailConfig;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
    }

    public void sendEmailFromTemplate(MailDto message, String[] messageConstant, String[] titleArgs, String[] contentArgs) {
        if (message.getEmail() == null) {
            log.debug("Email id is required for sending email.");
            return;
        }
        /*
         * Whatever locale is chosen, respective message.properties file will be used for email body
         */
        Locale locale = Locale.forLanguageTag(message.getLangKey() != null ? message.getLangKey() : "en");
        message.setWebsiteUrl(appMailConfig.getUiUrl());
        message.setTwitterUrl(appMailConfig.getTwitterUrl());
        message.setLinkedinUrl(appMailConfig.getLinkedInUrl());
        log.info(
                "Social Urls configured for \nwebsite: {}\ntwitter: {}\nlinkedin: {}",
                message.getWebsiteUrl(),
                message.getTwitterUrl(),
                message.getLinkedinUrl()
        );
        try {
            String action = messageConstant.length < 3 ? "login" : messageSource.getMessage(messageConstant[2], null, locale);
            String subject = messageSource.getMessage(messageConstant[0], titleArgs, locale);
            String content = messageSource.getMessage(messageConstant[1], contentArgs, locale);
            if (message.getActionUrl() == null) message.setActionUrl(message.getWebsiteUrl());
            message.setTitle(subject);
            String htmlMessage = HtmlTemplates.getCommonTemplate(message, content, action);
            sendEmail(message.getEmail(), subject, htmlMessage, null, null);
        } catch (Exception e) {
            log.error("Exception while sending email to user '{}'", message.getEmail(), e);
            e.printStackTrace();
        }
    }

    public void sendEmailFromTemplate(MailDto message, String[] messageConstant, String[] titleArgs,
                                      String[] contentArgs, ByteArrayResource attachment, String fileName) {
        if (message.getEmail() == null) {
            log.debug("Email id is required for sending email.");
            return;
        }
        /*
         * Whatever locale is chosen, respective message.properties file will be used for email body
         */
        Locale locale = Locale.forLanguageTag(message.getLangKey() != null ? message.getLangKey() : "en");
        message.setWebsiteUrl(appMailConfig.getUiUrl());
        message.setTwitterUrl(appMailConfig.getTwitterUrl());
        message.setLinkedinUrl(appMailConfig.getLinkedInUrl());
        log.info(
                "Social Urls configured for \nwebsite: {}\ntwitter: {}\nlinkedin: {}",
                message.getWebsiteUrl(),
                message.getTwitterUrl(),
                message.getLinkedinUrl()
        );
        try {
            String action = messageSource.getMessage(messageConstant[2], null, locale);
            String subject = messageSource.getMessage(messageConstant[0], titleArgs, locale);
            String content = messageSource.getMessage(messageConstant[1], contentArgs, locale);
            message.setTitle(subject);
            String htmlMessage = HtmlTemplates.getCommonTemplate(message, content, action);
            sendEmail(message.getEmail(), subject, htmlMessage, attachment, fileName);
        } catch (Exception e) {
            log.error("Exception while sending email to user '{}'", message.getEmail(), e);
            e.printStackTrace();
        }
    }

    private void sendEmail(String to, String subject, String content, ByteArrayResource attachment, String fileName) {
        log.debug("Send email to '{}' with subject '{}'", to, subject);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(String.format("%s<%s>", appMailConfig.getSenderName(), appMailConfig.getSenderEmail()));
            message.setSubject(subject);
            message.setText(content, true);
            if (attachment != null) {
                message.addAttachment(attachment.getFilename() == null ? fileName : attachment.getFilename(), attachment);
            }
            // Prepare the evaluation context
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            log.warn("Email could not be sent to user '{}'", to, e);
        }
    }

}
