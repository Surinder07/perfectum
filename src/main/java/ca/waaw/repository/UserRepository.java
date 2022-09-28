package ca.waaw.repository;

import ca.waaw.domain.User;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<User> findOneByEmailAndDeleteFlag(String email, boolean deleteFlag);

    Optional<User> findOneByUsernameAndDeleteFlag(String username, boolean deleteFlag);

    Optional<User> findOneByUsernameOrEmail(String username, String email);

    Optional<User> findOneByAuthority(Authority authority);

    Optional<List<User>> findAllByStatusAndCreatedDateBefore(EntityStatus status, Instant date);

    List<User> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

    List<User> findAllByEmailInAndDeleteFlag(List<String> email, boolean deleteFlag);

    List<User> findAllByLocationRoleIdAndDeleteFlag(String locationRoleId, boolean deleteFlag);

    Page<User> findAllByLocationRoleIdAndDeleteFlag(String locationRoleId, boolean deleteFlag, Pageable pageable);

    Page<User> searchUsersWithLocationRoleIdAndDeleteFlag(String searchKey, String locationRoleId, boolean deleteFlag, Pageable pageable);

}