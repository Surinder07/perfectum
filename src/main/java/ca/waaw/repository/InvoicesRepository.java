package ca.waaw.repository;

import ca.waaw.domain.Invoices;
import ca.waaw.enumration.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoicesRepository extends JpaRepository<Invoices, String> {

    @Query(value = "SELECT waaw_id from invoices WHERE waaw_id IS NOT NULL ORDER BY waaw_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

    Optional<Invoices> findOneByIdAndOrganizationId(String id, String organizationId);

    Page<Invoices> findAllByOrganizationId(String organizationId, Pageable pageable);

    Optional<Invoices> findOneByOrganizationIdAndInvoiceStatus(String organizationId, InvoiceStatus status);

}