package ca.waaw.repository;

import ca.waaw.domain.ShiftsBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftsBatchRepository extends JpaRepository<ShiftsBatch, String> {

    Optional<ShiftsBatch> findOneByIdAndDeleteFlag(String batchId, boolean deleteFlag);

    @Query(value = "SELECT name from shifts_batch WHERE name IS NOT NULL AND organization_id = ? " +
            "ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    Optional<String> getLastUsedName(String organizationId);

}
