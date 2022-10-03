package ca.waaw.repository;

import ca.waaw.domain.Shifts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftsRepository extends JpaRepository<Shifts, String> {

    Optional<Shifts> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartBetween(String userId, Instant startRange, Instant endRange);

    List<Shifts> findAllByUserIdAndDeleteFlagAndStartBetween(String userId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<Shifts> findAllByLocationIdAndStartBetween(String locationId, Instant startRange, Instant endRange);

    List<Shifts> findAllByLocationRoleIdAndStartBetween(String locationRoleId, Instant startRange, Instant endRange);

    List<Shifts> findAllByUserIdInAndStartBetween(List<String> userIds, Instant startRange, Instant endRange);

}
