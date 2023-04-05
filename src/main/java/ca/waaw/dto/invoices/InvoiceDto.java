package ca.waaw.dto.invoices;

import ca.waaw.enumration.Currency;
import ca.waaw.enumration.InvoiceStatus;
import ca.waaw.enumration.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {

    private String id;

    private String waawId;

    private String stripeId;

    private String organizationId;

    private int quantity;

    private float unitPrice;

    private TransactionType transactionType;

    private float totalAmount;

    private Currency currency;

    private String invoiceDate;

    private String dateRange;

    private InvoiceStatus invoiceStatus;

    private String dueDate;

    private String paymentDate;

    private String invoiceUrl;

}