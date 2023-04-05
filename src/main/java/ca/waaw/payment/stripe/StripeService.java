package ca.waaw.payment.stripe;

import ca.waaw.config.applicationconfig.AppPaymentConfig;
import ca.waaw.dto.CreditCardDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class StripeService {

    private final Logger log = LogManager.getLogger(StripeService.class);

    private final AppPaymentConfig paymentConfig;

    public String createNewCustomer(String email, String name, String mobile) throws StripeException {
        Stripe.apiKey = paymentConfig.getApiKey();
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .setPhone(mobile)
                .build();
        Customer customer = Customer.create(params);
        log.info("Successfully added {} to stripe with customer id: {}", email, customer.getId());
        return customer.getId();
    }

    public String createNewSetupIntent(String customerId) throws StripeException {
        Stripe.apiKey = paymentConfig.getApiKey();
        SetupIntentCreateParams params = SetupIntentCreateParams
                .builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build();
        SetupIntent setupIntent = SetupIntent.create(params);
        return setupIntent.getClientSecret();
    }

    public void createNewCard(String tokenId, String customerId) throws StripeException {
        Stripe.apiKey = paymentConfig.getApiKey();
        CustomerRetrieveParams retrieveParams = CustomerRetrieveParams.builder()
                .addExpand("sources")
                .build();
        Customer customer = Customer.retrieve(customerId, retrieveParams, null);
        Map<String, Object> cardParams = new HashMap<>();
        cardParams.put("source", tokenId);

        Card card = (Card) customer.getSources().create(cardParams);
        log.info("Saved a new card with id ({}) for stripe customer: {}", card.getId(), customerId);
    }

    public void deleteCard(String cardId, String customerId) throws StripeException {
        Stripe.apiKey = paymentConfig.getApiKey();
        CustomerRetrieveParams retrieveParams = CustomerRetrieveParams.builder()
                .addExpand("sources")
                .build();
        Customer customer = Customer.retrieve(customerId, retrieveParams, null);
        Card card = (Card) customer.getSources().retrieve(cardId);
        card.delete();
    }

    public List<CreditCardDto> getAllCards(String customerId) throws StripeException {
        Stripe.apiKey = paymentConfig.getApiKey();
        CustomerRetrieveParams retrieveParams = CustomerRetrieveParams.builder()
                .addExpand("sources")
                .build();
        Customer customer = Customer.retrieve(customerId, retrieveParams, null);
        String defaultCard = customer.getDefaultSource();
        PaymentSourceCollectionListParams params = PaymentSourceCollectionListParams.builder()
                .setObject("card")
                .build();
        PaymentSourceCollection cards = customer.getSources().list(params);
        return cards.getData().stream()
                .map(paymentSource -> (Card) paymentSource)
                .map(card -> CreditCardDto.builder()
                        .id(card.getId())
                        .lastFourDigits(card.getLast4())
                        .brand(card.getBrand())
                        .expiryMonth(card.getExpMonth().intValue())
                        .expiryYear(card.getExpYear().intValue())
                        .cvcChecked(card.getCvcCheck().equalsIgnoreCase("pass"))
                        .isDefault(card.getId().equalsIgnoreCase(defaultCard))
                        .build()
                )
                .collect(Collectors.toList());
    }

    public String createNewPaymentIntent(String customerId, float amount, String currency, String invoiceId) throws StripeException {
        Stripe.apiKey = paymentConfig.getApiKey();
        if (currency == null) currency = "CAD";
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams
                        .builder()
                        .setAmount((long) (amount * 100))
                        .setCurrency(currency)
                        .addPaymentMethodType("card")
                        .putMetadata("invoiceId", invoiceId)
                        .setCustomer(customerId)
                        .build();
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.info("Successfully created a payment intent for customer({}) for a amount of {} {}", customerId, amount, currency);
        return paymentIntent.getClientSecret();
    }

    public static void main(String[] args) throws StripeException {
        Stripe.apiKey = "sk_test_51IuozbEkZ4NGJcE2m9MpdvSkZbDTFlR2ot3a9vSsQG0QHdwOBKtV9s0pQtKiGN1y3jvMM5ED3dNtVOBxUAMKMxZn00aU9CKJdR";
        CustomerRetrieveParams retrieveParams = CustomerRetrieveParams.builder()
                .addExpand("sources")
                .build();
        Customer customer = Customer.retrieve("cus_NV1B10NztRL1Lj", retrieveParams, null);
        PaymentSourceCollectionListParams params = PaymentSourceCollectionListParams.builder()
                .setObject("card")
                .build();
        PaymentSourceCollection cards = customer.getSources().list(params);
        cards.getData().stream()
                .map(paymentSource -> (Card) paymentSource)
                .forEach(System.out::println);
        PaymentIntentCreateParams params1 =
                PaymentIntentCreateParams
                        .builder()
                        .setAmount(1099L)
                        .setCurrency("usd")
                        .addPaymentMethodType("card")
                        .setCustomer("")
                        .build();
    }

}