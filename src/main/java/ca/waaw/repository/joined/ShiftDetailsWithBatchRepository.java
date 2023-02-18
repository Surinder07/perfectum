package ca.waaw.repository.joined;

import ca.waaw.domain.joined.ShiftDetailsWithBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ShiftDetailsWithBatchRepository extends JpaRepository<ShiftDetailsWithBatch, String> {

    @Query(value = "SELECT s FROM ShiftDetailsWithBatch s WHERE (?1 IS NULL OR (s.batch.name LIKE " +
            "CONCAT('%', ?1, '%') OR s.batch.waawId LIKE CONCAT('%', ?1, '%'))) AND (?2 IS NULL OR " +
            "s.shiftStatus = ?2) AND s.userId = ?3 AND ((?4 IS NULL OR ?5 IS NULL) OR " +
            "(s.start BETWEEN ?4 AND ?5 OR s.end BETWEEN ?4 AND ?5)) AND s.deleteFlag = FALSE " +
            "ORDER BY s.start DESC")
    Page<ShiftDetailsWithBatch> searchAndFilterShiftsDate(String searchKey, String status, String userId,
                                                          Instant start, Instant end, Pageable pageable);

    @Query(value = "SELECT s FROM ShiftDetailsWithBatch s WHERE (?2 IS NULL OR s.start BETWEEN ?1 AND ?2) " +
            "AND (?2 IS NOT NULL OR s.start > ?1) AND (?3 = TRUE OR s.locationRole.adminRights = FALSE) " +
            "AND s.organizationId = ?4 AND (?5 IS NULL OR s.locationId = ?5) AND (?6 IS NULL OR s.userId = ?6) " +
            "AND s.deleteFlag = FALSE AND s.shiftStatus = 'RELEASED' ORDER BY s.start ASC")
    Page<ShiftDetailsWithBatch> getShiftsForDashboard(Instant todayStart, Instant todayEnd, boolean admin,
                                                      String organizationId, String locationId, String userId, Pageable pageable);

}