package ca.waaw.repository;

import ca.waaw.domain.joined.DetailedShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DetailedShiftRepository extends JpaRepository<DetailedShift, String> {

    List<DetailedShift> findAllByUser_idInAndDeleteFlagAndStartBetween(List<String> userId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<DetailedShift> findAllByOrganizationIdAndDeleteFlagAndStartBetween(String organizationId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<DetailedShift> findAllByLocation_idAndDeleteFlagAndStartBetween(String locationId, boolean deleteFlag, Instant startRange, Instant endRange);

    List<DetailedShift> findAllByLocationRole_idAndDeleteFlagAndStartBetween(String locationRoleId, boolean deleteFlag, Instant startRange, Instant endRange);

}
