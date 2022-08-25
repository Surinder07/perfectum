package ca.waaw.repository;

import ca.waaw.domain.Shifts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftsRepository extends JpaRepository<Shifts, String> {
}
