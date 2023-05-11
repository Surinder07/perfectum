package ca.waaw.repository.user;

import ca.waaw.domain.user.User;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT waaw_custom_id from user WHERE waaw_custom_id IS NOT NULL ORDER BY waaw_custom_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

    Optional<User> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<User> findOneByEmailAndDeleteFlag(String email, boolean deleteFlag);

    Optional<User> findOneByUsernameAndDeleteFlag(String username, boolean deleteFlag);

    @Query(value = "SELECT u FROM User u WHERE (u.username = ?1 OR u.email = ?1) AND u.deleteFlag = FALSE")
    Optional<User> getByUsernameOrEmail(String login);

    Optional<User> findOneByAuthority(Authority authority);

    Optional<User> findOneByAuthorityAndOrganizationId(Authority authority, String organizationId);

    List<User> findAllByAuthorityAndLocationIdAndDeleteFlag(Authority authority, String locationId, boolean deleteFlag);

    List<User> findAllByAuthorityAndOrganizationIdAndDeleteFlag(Authority authority, String organizationId, boolean deleteFlag);

    List<User> findAllByLocationIdInAndAuthorityAndDeleteFlag(Set<String> locationId, Authority authority, boolean deleteFlag);

    List<User> findAllByOrganizationIdInAndAuthorityAndDeleteFlag(Set<String> organizationIds, Authority authority, boolean deleteFlag);
    
    List<User> findAllByOrganizationIdInAndAuthorityInAndDeleteFlag(List<String> organizationIds, List<Authority> authorities, boolean deleteFlag);

    Optional<List<User>> findAllByAccountStatusAndCreatedDateBefore(AccountStatus status, Instant date);

    List<User> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);

    List<User> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

    List<User> findAllByEmailInAndDeleteFlag(List<String> email, boolean deleteFlag);

    List<User> findAllByIdInAndDeleteFlag(List<String> ids, boolean deleteFlag);

    List<User> findAllByLocationRoleIdAndDeleteFlag(String locationRoleId, boolean deleteFlag);

    List<User> findAllByAccountStatusAndOrganizationIdAndDeleteFlag(AccountStatus status, String organizationId, boolean deleteFlag);
    
    List<User> findAllByOrganizationIdAndAuthorityInAndDeleteFlag(String organizationId, List<Authority> authorities, boolean deleteFlag);

}