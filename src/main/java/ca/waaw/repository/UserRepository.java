package ca.waaw.repository;

import ca.waaw.domain.User;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.Authority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT name from shifts_batch WHERE name IS NOT NULL ORDER BY created_date DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

    Optional<User> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<User> findOneByEmailAndDeleteFlag(String email, boolean deleteFlag);

    Optional<User> findOneByUsernameAndDeleteFlag(String username, boolean deleteFlag);

    Optional<User> findOneByUsernameOrEmail(String username, String email);

    Optional<User> findOneByAuthority(Authority authority);

    Optional<List<User>> findAllByAccountStatusAndCreatedDateBefore(AccountStatus status, Instant date);

    List<User> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

    List<User> findAllByEmailInAndDeleteFlag(List<String> email, boolean deleteFlag);

    List<User> findAllByLocationRoleIdAndDeleteFlag(String locationRoleId, boolean deleteFlag);

    List<User> findAllByIdInAndDeleteFlag(List<String> id, boolean deleteFlag);

    Page<User> findAllByLocationRoleIdAndDeleteFlag(String locationRoleId, boolean deleteFlag, Pageable pageable);

    Page<User> searchUsersWithLocationRoleIdAndDeleteFlag(String searchKey, String locationRoleId, boolean deleteFlag, Pageable pageable);

}