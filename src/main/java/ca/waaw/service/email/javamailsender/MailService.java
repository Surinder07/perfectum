package ca.waaw.service.email.javamailsender;

import ca.waaw.config.applicationconfig.AppMailConfig;
import ca.waaw.dto.MailDto;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.activation.MimetypesFileTypeMap;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending mails
 */
@SuppressWarnings("unused")
@Service
public class MailService {

    @Value(value = "classpath:images/logo.png")
    private Resource logoImageResource;

    @Value(value = "classpath:images/twitter.png")
    private Resource twitterImageResource;

    @Value(value = "classpath:images/linkedin.png")
    private Resource linkedinImageResource;

    private final Logger log = LogManager.getLogger(MailService.class);

    private static final String DTO = "dto";

    private final AppMailConfig appMailConfig;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    public MailService(AppMailConfig appMailConfig,
                       JavaMailSender javaMailSender,
                       MessageSource messageSource,
                       SpringTemplateEngine templateEngine) {
        this.appMailConfig = appMailConfig;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    /**
     * @param message      All details related to the message and recipient
     * @param templateName html template address relative to templates folder without extension
     *                     example -> "mail/activateEmail"
     * @param titleKey     key from {@code message.properties} corresponding to the subject of mail
     * @param args         any arguments if needed in the subject
     *                     example -> Subject: "{0} invites you to join", args: "Pragra"
     */
    public void sendEmailFromTemplate(MailDto message, String templateName, String titleKey, String... args) {
        CompletableFuture.runAsync(() -> {
            if (message.getEmail() == null) {
                log.debug("Email id is required for sending email.");
                return;
            }
            /*
             * Whatever locale is chosen, respective message.properties file will be used for email body
             */
            Locale locale = Locale.forLanguageTag(message.getLangKey() != null ? message.getLangKey() : "en");
            Context context = new Context(locale);
            message.setWebsiteUrl(appMailConfig.getUiUrl());
            message.setTwitterUrl(appMailConfig.getTwitterUrl());
            message.setLinkedinUrl(appMailConfig.getLinkedInUrl());
            log.info(
                    "Social Urls configured for \nwebsite: {}\ntwitter: {}\nlinkedin: {}",
                    message.getWebsiteUrl(),
                    message.getTwitterUrl(),
                    message.getLinkedinUrl()
            );
            /*
             * these variables can be accessed inside the email simply like ${dto.message.name}. Here dto is the variable
             * we have added, then message is an object inside that object and name is a String in message.
             *
             * By setting our images as variables we can use them inside our mail design instead of attachments
             * inside your html set src="logo.png" th:src="|cid:${logo}|" in you img tag
             */
            context.setVariable(DTO, message);
            context.setVariable("logo", logoImageResource.getFilename());
            context.setVariable("twitter", twitterImageResource.getFilename());
            context.setVariable("linkedin", linkedinImageResource.getFilename());

            String content = templateEngine.process(templateName, context);
            String subject = messageSource.getMessage(titleKey, args.length == 0 ? null : args, locale);
            sendEmail(message.getEmail(), subject, content);
        });
    }

    private void sendEmail(String to, String subject, String content) {
        log.debug("Send email to '{}' with subject '{}'", to, subject);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(String.format("%s<%s>", appMailConfig.getSenderName(), appMailConfig.getSenderEmail()));
            message.setSubject(subject);
            message.setText(content, true);
            addInlineImagesToMessage(message);
            // Prepare the evaluation context
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            log.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    /**
     * This method will add all our images (logo, social icons, etc.) to the message as attachments
     *
     * @param message the message object to be sent
     */
    private void addInlineImagesToMessage(MimeMessageHelper message) {
        try {
            String logoName = logoImageResource.getFilename();
            String logoContentType = new MimetypesFileTypeMap().getContentType(logoImageResource.getFile());
            final InputStreamSource logoSource = new ByteArrayResource(IOUtils.toByteArray(logoImageResource.getInputStream()));
            String twitterName = twitterImageResource.getFilename();
            String twitterContentType = new MimetypesFileTypeMap().getContentType(twitterImageResource.getFile());
            final InputStreamSource twitterSource = new ByteArrayResource(IOUtils.toByteArray(twitterImageResource.getInputStream()));
            String linkedinName = linkedinImageResource.getFilename();
            String linkedinContentType = new MimetypesFileTypeMap().getContentType(linkedinImageResource.getFile());
            final InputStreamSource linkedinSource = new ByteArrayResource(IOUtils.toByteArray(linkedinImageResource.getInputStream()));
            log.info("Found images: {}, {}, {}", logoName, twitterName, linkedinName);
            assert logoName != null;
            message.addInline(logoName, logoSource, logoContentType);
            assert twitterName != null;
            message.addInline(twitterName, twitterSource, twitterContentType);
            assert linkedinName != null;
            message.addInline(linkedinName, linkedinSource, linkedinContentType);
        } catch (Exception e) {
            log.error("Exception while trying to read images for email.");
        }
    }
}
