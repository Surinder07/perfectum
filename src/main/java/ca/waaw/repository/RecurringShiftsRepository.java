package ca.waaw.repository;

import ca.waaw.domain.RecurringShifts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecurringShiftsRepository extends JpaRepository<RecurringShifts, String> {
}
