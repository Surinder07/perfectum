package ca.waaw.repository;

import ca.waaw.domain.EmployeePreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeePreferencesRepository extends JpaRepository<EmployeePreferences, String> {

    Optional<EmployeePreferences> findOneByUserIdAndIsExpired(String userId, boolean isExpired);

    List<EmployeePreferences> findAllByUserId(String userId);

}
