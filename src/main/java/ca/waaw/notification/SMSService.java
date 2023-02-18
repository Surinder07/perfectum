package ca.waaw.notification;

import ca.waaw.config.applicationconfig.AppAzureConfig;
import com.azure.communication.sms.SmsClient;
import com.azure.communication.sms.SmsClientBuilder;
import com.azure.communication.sms.models.SmsSendResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

@Service
public class SMSService {

	private final Logger LOGGER = LogManager.getLogger(SMSService.class);

	private AzureKeyCredential keyCredential;

	@Autowired
	private AppAzureConfig appAzureConfig;

	@Autowired
	private MessageSource messageSource;

	BiFunction<String, Locale, String> messageBuilder = new BiFunction<>() {
		@Override
		public String apply(String t, Locale u) {
			return messageSource.getMessage(t, null, u);
		}
	};

	private SmsClient getSMSClient() {
		return new SmsClientBuilder()
				.endpoint(appAzureConfig.getSms().getEndPoint())
				.credential(new AzureKeyCredential(appAzureConfig.getSms().getKeyCredential()))
				.buildClient();
	}
    
	/**
	 * Method will be used to send one to one message
	 * @param to recipient phone number
	 * @param msgKey message key to fetch message details from message.properties file
	 */
	public void sendOneToOneSMS(String to, String msgKey) {
		CompletableFuture.runAsync(() -> {
			try {
				SmsSendResult sendResult = getSMSClient().send(appAzureConfig.getSms().getFromMobile(), to,
						messageBuilder.apply(msgKey, Locale.ENGLISH));
				LOGGER.info("Response code: {}", sendResult.getHttpStatusCode());
				LOGGER.info("Message Id: {}, Recipient Number: {}, Send Result Successful:{}",
						sendResult.getMessageId(), sendResult.getTo(), sendResult.isSuccessful());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
    
	/**
	 * Method will be used to send same message to many users
	 * @param to recipient phone number
	 * @param msgKey message key to fetch message details from message.properties file
	 */
	public void sendOneToManySMS(String from, List<String> to, String msgKey) {
		CompletableFuture.runAsync(() -> {
			Iterable<SmsSendResult> sendResults = getSMSClient().sendWithResponse(appAzureConfig.getSms().getFromMobile(),
					to, messageBuilder.apply(msgKey, Locale.ENGLISH), null, Context.NONE).getValue();
			sendResults.forEach(sendResult -> {
				LOGGER.info("Response code: {}", sendResult.getHttpStatusCode());
				LOGGER.info("Message Id: " + sendResult.getMessageId() + " Recipient Number: " + sendResult.getTo() + " Send Result Successful:" + sendResult.isSuccessful());
			});
		});
	}

}
