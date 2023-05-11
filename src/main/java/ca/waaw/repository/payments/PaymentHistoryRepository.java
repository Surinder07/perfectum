package ca.waaw.repository.payments;

import ca.waaw.domain.payments.PaymentHistory;
import ca.waaw.enumration.payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, String> {

    @Query(value = "SELECT invoice_id from payment_history WHERE invoice_id IS NOT NULL ORDER BY invoice_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedInvoiceId();

    @Query(value = "SELECT transaction_id from payment_history WHERE transaction_id IS NOT NULL ORDER BY transaction_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedTransactionId();

    Optional<PaymentHistory> findOneByIdAndOrganizationId(String id, String organizationId);

    Page<PaymentHistory> findAllByOrganizationId(String organizationId, Pageable pageable);

    Optional<PaymentHistory> findOneByOrganizationIdAndPaymentStatus(String organizationId, PaymentStatus status);

}