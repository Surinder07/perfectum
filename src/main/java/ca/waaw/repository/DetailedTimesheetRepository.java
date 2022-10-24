package ca.waaw.repository;

import ca.waaw.domain.joined.DetailedTimesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DetailedTimesheetRepository extends JpaRepository<DetailedTimesheet, String> {

    List<DetailedTimesheet> getByOrganizationIdAndDates(String organizationId, Instant start, Instant end);

    List<DetailedTimesheet> getByLocationIdAndDates(String locationId, Instant start, Instant end);

    List<DetailedTimesheet> getByUserIdAndDates(String userId, Instant start, Instant end);

}