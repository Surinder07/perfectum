package ca.waaw.repository;

import ca.waaw.domain.ShiftBatchUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftBatchMappedUserRepository extends JpaRepository<ShiftBatchUserMapping, String> {
}
