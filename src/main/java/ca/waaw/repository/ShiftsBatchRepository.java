package ca.waaw.repository;

import ca.waaw.domain.ShiftsBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftsBatchRepository extends JpaRepository<ShiftsBatch, String> {

    Optional<ShiftsBatch> findOneByIdAndDeleteFlag(String batchId, boolean deleteFlag);

    @Query(value = "SELECT waaw_id from shifts_batch WHERE waaw_id IS NOT NULL AND organization_id = ? " +
            "ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    Optional<String> getLastUsedId(String organizationId);

    List<ShiftsBatch> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);

    List<ShiftsBatch> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

}