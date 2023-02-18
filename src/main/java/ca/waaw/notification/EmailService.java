package ca.waaw.notification;

import ca.waaw.config.applicationconfig.AppAzureConfig;
import com.google.gson.Gson;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AllArgsConstructor
public class EmailService {

    private final Logger log = LogManager.getLogger(EmailService.class);

    private final AppAzureConfig appAzureConfig;

    public void sendSimpleMail(String toEmail, String subject, String body) {
        Content content = new Content("text/html", body);
        sendEmail(new Email(toEmail), subject, content, null);
    }

    public void sendEmail(Email to, String subject, Content body, Email bcc) {
        Mail mail;
        if (bcc != null) {
            Personalization personalization = new Personalization();
            personalization.addBcc(bcc);
            personalization.addTo(to);
            personalization.setFrom(new Email(appAzureConfig.getSendGrid().getSenderEmail(), appAzureConfig.getSendGrid().getSenderName()));
            personalization.setSubject(subject);
            mail = new Mail();
            mail.addContent(body);
            mail.addPersonalization(personalization);
        } else {
            mail = new Mail(new Email(appAzureConfig.getSendGrid().getSenderEmail(), appAzureConfig.getSendGrid().getSenderName()), subject, to, body);
        }

        SendGrid sendGrid = new SendGrid(appAzureConfig.getSendGrid().getApiKey());

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            log.info("Email service response code: " + response.getStatusCode());
            if (StringUtils.isNotEmpty(response.getBody())) {
                log.info("Email service response: " + new Gson().toJson(response.getBody()));
            }

        } catch (IOException ex) {
            log.debug("Exception while sending mail: " + ex.getMessage());
        }
    }

}
