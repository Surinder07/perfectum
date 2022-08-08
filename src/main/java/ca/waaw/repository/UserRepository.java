package ca.waaw.repository;

import ca.waaw.domain.User;
import ca.waaw.enumration.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findOneByEmailAndDeleteFlag(String email, boolean deleteFlag);
    Optional<User> findOneByUsernameAndDeleteFlag(String email, boolean deleteFlag);
    Optional<User> findOneByUsernameOrEmail(String username, String email);
    Optional<User> findOneByActivationKey(String key);
    Optional<User> findOneByResetKey(String key);
    Optional<User> findOneByInviteKey(String key);
    Optional<List<User>> findAllByStatusAndCreatedDateBefore(EntityStatus status, Instant date);
}
