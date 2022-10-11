package ca.waaw.repository;

import ca.waaw.domain.UserTokens;
import ca.waaw.enumration.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserTokens, String> {

    Optional<UserTokens> findOneByTokenAndTokenTypeAndIsExpired(String token, UserToken type, boolean isExpired);

    Optional<List<UserTokens>> findAllByUserIdInAndTokenType(List<String> userIds, UserToken type);

    List<UserTokens> findAllByIsExpired(boolean isExpired);

}
