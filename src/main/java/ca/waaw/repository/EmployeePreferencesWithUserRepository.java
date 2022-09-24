package ca.waaw.repository;

import ca.waaw.domain.joined.EmployeePreferencesWithUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePreferencesWithUserRepository extends JpaRepository<EmployeePreferencesWithUser, String> {

    Optional<EmployeePreferencesWithUser> findOneByUserIdAndIsExpired(String userId, boolean isExpired);

    List<EmployeePreferencesWithUser> findAllByUserId(String userId);

    List<EmployeePreferencesWithUser> findAllByLocationIdAndIsExpiredAndDeleteFlag(String locationId, boolean isExpired, boolean deleteFlag);

    List<EmployeePreferencesWithUser> findAllByLocationRoleIdAndIsExpiredAndDeleteFlag(String locationRoleId, boolean isExpired, boolean deleteFlag);

    List<EmployeePreferencesWithUser> findAllByIsExpiredAndDeleteFlagAndUserIdIn(boolean isExpired, boolean deleteFlag, List<String> userIds);

}
