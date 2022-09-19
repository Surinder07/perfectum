package ca.waaw.repository;

import ca.waaw.domain.EmployeePreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePreferencesRepository extends JpaRepository<EmployeePreferences, String> {

    Optional<EmployeePreferences> findOneByUserIdAndIsExpired(String userId, boolean isExpired);

    List<EmployeePreferences> findAllByUserId(String userId);

}
