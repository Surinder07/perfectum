package ca.waaw.domain.payments;

import ca.waaw.enumration.Currency;
import ca.waaw.enumration.payment.PaymentStatus;
import ca.waaw.enumration.payment.TransactionType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "payment_history")
public class PaymentHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "invoice_id")
    private String invoiceId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "stripe_id")
    private String stripeId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column
    private int quantity;

    @Column(name = "unit_price")
    private float unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "total_amount")
    private float totalAmount;

    @Column
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "invoice_date")
    private Instant invoiceDate;

    @Column(name = "invoice_start")
    private Instant invoiceStart;

    @Column(name = "invoice_end")
    private Instant invoiceEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "payment_date")
    private Instant paymentDate;

    @Column(name = "invoice_url")
    private String invoiceUrl;

}