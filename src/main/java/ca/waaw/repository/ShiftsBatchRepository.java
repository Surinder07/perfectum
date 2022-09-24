package ca.waaw.repository;

import ca.waaw.domain.ShiftsBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ShiftsBatchRepository extends JpaRepository<ShiftsBatch, String> {

    List<ShiftsBatch> getOverlappingBatchForLocationId(String locationId, Instant start, Instant end);

}
