package ca.waaw.repository;

import ca.waaw.domain.ShiftBatchMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftBatchMappedUserAndRoleRepository extends JpaRepository<ShiftBatchMapping, String> {
}