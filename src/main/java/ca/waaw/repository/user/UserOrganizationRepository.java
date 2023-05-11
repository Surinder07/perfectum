package ca.waaw.repository.user;

import ca.waaw.domain.user.UserOrganization;
import ca.waaw.enumration.user.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {

    Optional<UserOrganization> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = "SELECT uo FROM UserOrganization uo WHERE uo.deleteFlag = FALSE AND uo.organization.deleteFlag = " +
            "FALSE AND uo.organization.paymentPending = FALSE AND uo.authority = 'ADMIN'")
    List<UserOrganization> getAllActiveOrganizationWithAdmin();

    @Query(value = "SELECT uo FROM UserOrganization uo WHERE uo.organization.trialEndDate > CURRENT_TIMESTAMP AND " +
            "uo.deleteFlag = FALSE AND uo.organization.deleteFlag = FALSE AND uo.authority = 'ADMIN'")
    List<UserOrganization> getAllTrialEndingWithAdmin();

    List<UserOrganization> findAllByDeleteFlagAndIdIn(boolean deleteFlag, List<String> ids);

    List<UserOrganization> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);

    Optional<UserOrganization> findOneByUsernameAndDeleteFlag(String username, boolean deleteFlag);

    /**
     * If a value is passed in any of the below options it will be considered or all data will be shown
     * loggedUserId is passed to ignore the logged-user in listing
     */
    Page<UserOrganization> searchAndFilterUsers(String searchKey, String organizationId, String locationId, String locationRoleId,
                                    Boolean isFullTime, AccountStatus status, boolean manager, Pageable pageable);

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