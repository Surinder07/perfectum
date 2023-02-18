package ca.waaw.repository;

import ca.waaw.domain.Reports;
import ca.waaw.enumration.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ReportsRepository extends JpaRepository<Reports, String> {

    @Query(value = "SELECT waaw_id from reports WHERE waaw_id IS NOT NULL ORDER BY waaw_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

    Optional<Reports> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = "SELECT r FROM Reports r WHERE r.organizationId = ?1 AND (?2 IS NULL OR r.locationId = ?2) " +
            "AND r.showToManger = ?3 AND ((?4 IS NULL OR ?5 IS NULL) OR r.createdDate BETWEEN ?4 AND ?5) " +
            "AND r.type = ?6 AND r.deleteFlag = FALSE ORDER BY r.createdDate DESC")
    Page<Reports> getAllWithFilters(String organizationId, String locationId, boolean isManager, Instant start,
                                    Instant end, UserReport type, Pageable pageable);

}