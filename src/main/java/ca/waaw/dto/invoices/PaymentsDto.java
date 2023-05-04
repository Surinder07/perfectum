package ca.waaw.dto.invoices;

import ca.waaw.enumration.Currency;
import ca.waaw.enumration.PaymentStatus;
import ca.waaw.enumration.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsDto {

    private String id;

    private String invoiceId;

    private String transactionId;

    private String stripeId;

    private String organizationId;

    private int quantity;

    private float unitPrice;

    private TransactionType transactionType;

    private float totalAmount;

    private Currency currency;

    private String invoiceDate;

    private String dateRange;

    private PaymentStatus paymentStatus;

    private String dueDate;

    private String paymentDate;

    private String invoiceUrl;

}