package ca.waaw.repository;

import ca.waaw.domain.joined.EmployeePreferencesWithUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeePreferencesWithUserRepository extends JpaRepository<EmployeePreferencesWithUser, String> {

    List<EmployeePreferencesWithUser> findAllByOrganizationIdAndIsExpiredAndDeleteFlag(String organizationId, boolean isExpired, boolean deleteFlag);

    List<EmployeePreferencesWithUser> findAllByLocationIdAndIsExpiredAndDeleteFlag(String locationId, boolean isExpired, boolean deleteFlag);

    List<EmployeePreferencesWithUser> findAllByLocationRoleIdInAndIsExpiredAndDeleteFlag(List<String> locationRoleIds, boolean isExpired, boolean deleteFlag);

    List<EmployeePreferencesWithUser> findAllByIsExpiredAndDeleteFlagAndUserIdIn(boolean isExpired, boolean deleteFlag, List<String> userIds);

}