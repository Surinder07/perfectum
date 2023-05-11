package ca.waaw.repository.shifts;

import ca.waaw.domain.shifts.ShiftDetails;
import ca.waaw.web.rest.utils.jpasqlqueries.ShiftQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftDetailsRepository extends JpaRepository<ShiftDetails, String> {

    Optional<List<ShiftDetails>> findAllByOrganizationIdAndBatchIdAndDeleteFlag(String organizationId, String batchId, boolean deleteFlag);

    Optional<List<ShiftDetails>> findAllByLocationIdAndBatchIdAndDeleteFlag(String locationId, String batchId, boolean deleteFlag);

    Optional<ShiftDetails> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = ShiftQueries.searchAndFilterShifts)
    List<ShiftDetails> searchAndFilterShifts(String searchKey, String locationId, String locationRoleId, String status,
                                             String userId, boolean isAdmin, Instant start, Instant end, List<String> batchIds);

    @Query(value = ShiftQueries.searchAndFilterShiftByUser)
    Page<ShiftDetails> searchAndFilterShifts(String searchKey, String status, String userId, Instant start,
                                             Instant end, Pageable pageable);

    @Query(value = ShiftQueries.getShiftsForDashboard)
    Page<ShiftDetails> getShiftsForDashboard(Instant todayStart, Instant todayEnd, boolean admin, String organizationId,
                                             String locationId, String userId, Pageable pageable);

}