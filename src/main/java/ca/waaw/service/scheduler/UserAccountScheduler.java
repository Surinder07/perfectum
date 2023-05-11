package ca.waaw.service.scheduler;

import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.user.User;
import ca.waaw.domain.user.UserTokens;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.UserTokenType;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserTokenRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class UserAccountScheduler {

    private final Logger log = LogManager.getLogger(UserAccountScheduler.class);

    private final UserRepository userRepository;

    private final UserTokenRepository userTokenRepository;

    private final AppValidityTimeConfig appValidityTimeConfig;

    /**
     * Not activated users should be automatically deleted after days set in properties days.
     * <p>
     * This is scheduled to get fired every day, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
                .findAllByAccountStatusAndCreatedDateBefore(AccountStatus.EMAIL_PENDING, Instant.now()
                        .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))
                .ifPresent(users -> {
                    userTokenRepository.findAllByUserIdInAndTokenType(users.stream().map(User::getId)
                                    .collect(Collectors.toList()), UserTokenType.ACTIVATION)
                            .ifPresent(userTokenRepository::deleteAll);
                    userRepository.deleteAll(users);
                    log.info("Successfully Deleted users that are not activated for last {} days: {}",
                            appValidityTimeConfig.getActivationLink(), users);
                });
    }

    @Scheduled(cron = "0 5 1 * * ?")
    public void expireInviteAndPasswordRestToken() {
        List<UserTokens> expiredTokens = userTokenRepository.findAllByIsExpired(false)
                .stream()
                .filter(token -> (token.getTokenType().equals(UserTokenType.INVITE) && token.getCreatedDate().isBefore(Instant.now()
                        .minus(appValidityTimeConfig.getUserInvite(), ChronoUnit.DAYS))) ||
                        (token.getTokenType().equals(UserTokenType.RESET) && token.getCreatedDate().isBefore(Instant.now()
                                .minus(appValidityTimeConfig.getPasswordReset(), ChronoUnit.DAYS)))
                )
                .peek(token -> token.setExpired(true))
                .collect(Collectors.toList());
        userTokenRepository.saveAll(expiredTokens);
        log.info("Expired tokens updated: {}", expiredTokens);
    }

}
