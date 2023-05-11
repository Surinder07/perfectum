package ca.waaw.repository.timesheet;

import ca.waaw.domain.timesheet.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, String> {

    Optional<Timesheet> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<Timesheet> findOneByUserIdAndDeleteFlag(String userId, boolean deleteFlag);

    Optional<Timesheet> getByUserIdBetweenDates(String userId, Instant start, Instant end);

    List<Timesheet> getAllByUserIdBetweenDates(String userId, Instant start, Instant end);

    List<Timesheet> findAllByStartBetweenAndDeleteFlag(Instant startRange, Instant endRange, boolean deleteFlag);

    Optional<Timesheet> getActiveTimesheet(String userId);

    @Query(value = "SELECT t FROM Timesheet t WHERE t.userId = ?1 AND ((?2 IS NULL OR ?3 IS NULL) OR " +
            "t.start BETWEEN ?2 AND ?3) AND (?4 IS NULL OR t.type = ?4) AND t.deleteFlag = FALSE")
    Page<Timesheet> filterTimesheet(String userId, Instant start, Instant end, String type, Pageable pageable);

    List<Timesheet> findAllByLocationIdAndStartBetweenAndDeleteFlag(String locationId, Instant start, Instant end, boolean deleteFlag);

}