package ca.waaw.repository;

import ca.waaw.domain.joined.ShiftDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DetailedShiftRepository extends JpaRepository<ShiftDetails, String> {

    List<ShiftDetails> findAllByUser_idInAndDeleteFlagAndStartBetween(List<String> userId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<ShiftDetails> findAllByOrganizationIdAndDeleteFlagAndStartBetween(String organizationId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<ShiftDetails> findAllByLocation_idAndDeleteFlagAndStartBetween(String locationId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<ShiftDetails> findAllByLocationRole_idAndDeleteFlagAndStartBetween(String locationRoleId, boolean deleteFlag, Instant startRange, Instant endRange);

}
