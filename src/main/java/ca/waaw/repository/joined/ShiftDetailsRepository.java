package ca.waaw.repository.joined;

import ca.waaw.domain.joined.ShiftDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ShiftDetailsRepository extends JpaRepository<ShiftDetails, String> {

    List<ShiftDetails> searchAndFilterShifts(String searchKey, String locationId, String locationRoleId, String status,
                                             String userId, boolean isAdmin, List<String> batchIds);

    Page<ShiftDetails> searchAndFilterShiftsDate(String searchKey, String locationId, String locationRoleId, String status,
                                                 String userId, boolean isAdmin, Instant start, Instant end, Pageable pageable);

}