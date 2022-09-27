package ca.waaw.service.scheduler;

import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.User;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.enumration.UserToken;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.UserTokenRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
                .findAllByStatusAndCreatedDateBefore(EntityStatus.PENDING, Instant.now()
                        .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))
                .ifPresent(users -> {
                    userTokenRepository.findAllByUserIdInAndTokenType(users.stream().map(User::getId)
                                    .collect(Collectors.toList()), UserToken.ACTIVATION)
                            .ifPresent(userTokenRepository::deleteAll);
                    userRepository.deleteAll(users);
                    log.info("Successfully Deleted users that are not activated for last {} days: {}",
                            appValidityTimeConfig.getActivationLink(), users);
                });
    }

    // TODO expire reset password token
//    @Scheduled(cron = "0 5 1 * * ?")

    // TODO expire invite token
//    @Scheduled(cron = "0 10 1 * * ?")

}
