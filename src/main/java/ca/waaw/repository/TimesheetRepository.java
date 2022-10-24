package ca.waaw.repository;

import ca.waaw.domain.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, String> {

    Optional<Timesheet> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<Timesheet> findOneByUserIdAndDeleteFlag(String userId, boolean deleteFlag);

    Optional<Timesheet> getByUserIdBetweenDates(String userId, Instant start, Instant end);

    Optional<Timesheet> getActiveTimesheet(String userId);

}