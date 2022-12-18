package ca.waaw.repository;

import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.enumration.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {

    Optional<UserOrganization> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    List<UserOrganization> findAllByDeleteFlagAndIdIn(boolean deleteFlag, List<String> ids);

    Optional<UserOrganization> findOneByUsernameAndDeleteFlag(String username, boolean deleteFlag);

    /**
     * If a value is passed in any of the below options it will be considered or all data will be shown
     */
    Page<UserOrganization> searchAndFilterUsers(String searchKey, String organizationId, String locationId, String locationRoleId,
                                    Boolean isFullTime, AccountStatus status, Pageable pageable);

    /**
     * Location id and role are only considered if they have non-null values
     */
    Page<UserOrganization> findUsersWithOrganizationIdAndLocationIdAndDeleteFlagAndAuthority(String organizationId, String locationId, boolean deleteFlag, String authority, Pageable pageable);

    Page<UserOrganization> findUsersWithLocationIdAndDeleteFlagAndAuthority(String locationId, boolean deleteFlag, String authority, Pageable pageable);

    /**
     * Location id and role are only considered if they have non-null values
     */
    Page<UserOrganization> searchUsersWithOrganizationIdAndLocationIdAndDeleteFlagAndAuthority(String searchKey, String locationId, String organizationId, boolean deleteFlag, String authority, Pageable pageable);

    Page<UserOrganization> searchUsersWithLocationIdAndDeleteFlagAndAuthority(String searchKey, String locationId, boolean deleteFlag, String authority, Pageable pageable);

}