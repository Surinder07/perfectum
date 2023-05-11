package ca.waaw.repository.shifts;

import ca.waaw.domain.shifts.ShiftWithRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftWithRoleRepository extends JpaRepository<ShiftWithRole, String> {

    Optional<ShiftWithRole> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<List<ShiftWithRole>> findAllByBatchIdAndDeleteFlag(String batchId, boolean deleteFlag);

}