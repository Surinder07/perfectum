package ca.waaw.dto.invoices;

import ca.waaw.enumration.Currency;
import ca.waaw.enumration.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewPaymentDto {

    private String organizationId;

    private float totalAmount;

    private Currency currency;

    private int quantity;

    private float unitPrice;

    private Instant paymentDate;

    private TransactionType type;

}