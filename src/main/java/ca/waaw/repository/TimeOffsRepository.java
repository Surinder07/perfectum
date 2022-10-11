package ca.waaw.repository;

import ca.waaw.domain.TimeOffs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeOffsRepository extends JpaRepository<TimeOffs, String> {
}