package ca.waaw.repository.timesheet;

import ca.waaw.domain.timesheet.DetailedTimesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DetailedTimesheetRepository extends JpaRepository<DetailedTimesheet, String> {

    List<DetailedTimesheet> getByLocationIdAndDates(String locationId, Instant start, Instant end);

    List<DetailedTimesheet> getByUserIdAndDates(String userId, Instant start, Instant end);

    @Query(value = "SELECT COUNT(t) FROM DetailedTimesheet t WHERE t.end IS NULL AND t.organizationId = ?1 AND " +
            "(?2 IS NULL OR t.locationId = ?2) AND (?3 IS FALSE OR t.locationRole.adminRights = FALSE)")
    int getOnlineEmployeeCount(String organizationId, String locationId, boolean manager);

    @Query(value = "SELECT t FROM DetailedTimesheet t WHERE t.end IS NULL AND t.deleteFlag = FALSE")
    List<DetailedTimesheet> getAllActiveTimers();

}