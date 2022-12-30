package ca.waaw.repository.joined;

import ca.waaw.domain.joined.BatchDetails;
import ca.waaw.enumration.ShiftBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface BatchDetailsRepository extends JpaRepository<BatchDetails, String> {

    Page<BatchDetails> searchAndFilterShifts(String searchKey, String organizationId, String locationId, Instant startDate,
                                             Instant endDate, ShiftBatchStatus status, Pageable pageable);

}