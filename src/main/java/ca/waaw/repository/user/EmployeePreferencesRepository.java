package ca.waaw.repository.user;

import ca.waaw.domain.user.EmployeePreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeePreferencesRepository extends JpaRepository<EmployeePreferences, String> {

    Optional<EmployeePreferences> findOneByUserIdAndIsExpired(String userId, boolean isExpired);

}
