package ca.waaw.repository;

import ca.waaw.domain.TimeOffs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TimeOffsRepository extends JpaRepository<TimeOffs, String> {

    Optional<TimeOffs> getByUserIdBetweenDates(String userId, Instant start, Instant end);

    Optional<TimeOffs> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

}