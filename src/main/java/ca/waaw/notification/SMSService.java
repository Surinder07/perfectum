package ca.waaw.notification;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.azure.communication.sms.SmsClient;
import com.azure.communication.sms.SmsClientBuilder;
import com.azure.communication.sms.models.SmsSendResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;

@Service
@ConfigurationProperties("azure")
public class SMSService {

	private final Logger LOGGER = LogManager.getLogger(SMSService.class);

	private String endpoint;

	private AzureKeyCredential keyCredential;

	@Autowired
	private MessageSource messageSource;

	BiFunction<String, Locale, String> messageBuilder = new BiFunction<String, Locale, String>() {
		@Override
		public String apply(String t, Locale u) {
			return messageSource.getMessage(t, null, u);
		}
	};

	private SmsClient getSMSClient() {
		return new SmsClientBuilder()
				.endpoint(endpoint)
				.credential(keyCredential)
				.buildClient();
	}
    
	/**
	 * Method will be used to send one to one message
	 * @param from
	 * @param to
	 * @param msgKey
	 */
	public void sendOneToOneSMS(String from, String to, String msgKey) {
		CompletableFuture.runAsync(() -> {
			SmsSendResult sendResult = getSMSClient().send(from, to, messageBuilder.apply(msgKey, Locale.ENGLISH));
			LOGGER.info("Message Id: " + sendResult.getMessageId() + " Recipient Number: " + sendResult.getTo() + " Send Result Successful:" + sendResult.isSuccessful());
		});
	}
    
	/**
	 * Method will be use to send same message to many users
	 * @param from
	 * @param to
	 * @param msgKey
	 */
	public void sendOneToManySMS(String from, List<String> to, String msgKey) {
		CompletableFuture.runAsync(() -> {
			Iterable<SmsSendResult> sendResults = getSMSClient().sendWithResponse(from, to, messageBuilder.apply(msgKey, Locale.ENGLISH), null, Context.NONE).getValue();
			sendResults.forEach(sendResult -> {
				LOGGER.info("Message Id: " + sendResult.getMessageId() + " Recipient Number: " + sendResult.getTo() + " Send Result Successful:" + sendResult.isSuccessful());
			});
		});
	}

}
