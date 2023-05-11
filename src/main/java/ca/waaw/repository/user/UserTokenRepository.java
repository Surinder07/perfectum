package ca.waaw.repository.user;

import ca.waaw.domain.user.UserTokens;
import ca.waaw.enumration.user.UserTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserTokens, String> {

    Optional<UserTokens> findOneByTokenAndTokenTypeAndIsExpired(String token, UserTokenType type, boolean isExpired);

    Optional<UserTokens> findOneByUserIdAndTokenTypeAndIsExpired(String userId, UserTokenType type, boolean isExpired);

    List<UserTokens> findOneByUserIdAndTokenTypeOrderByCreatedDateDesc(String userId, UserTokenType type);

    Optional<List<UserTokens>> findAllByUserIdInAndTokenType(List<String> userIds, UserTokenType type);

    List<UserTokens> findAllByIsExpired(boolean isExpired);

}
