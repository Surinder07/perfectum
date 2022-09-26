package ca.waaw.repository;

import ca.waaw.domain.ShiftsBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftsBatchRepository extends JpaRepository<ShiftsBatch, String> {

    Optional<ShiftsBatch> findOneByIdAndDeleteFlag(String batchId, boolean deleteFlag);

    List<ShiftsBatch> getOverlappingBatchForLocationId(String locationId, Instant start, Instant end);

}
