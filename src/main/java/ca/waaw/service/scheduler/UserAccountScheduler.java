package ca.waaw.service.scheduler;

import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class UserAccountScheduler {

    private final Logger log = LogManager.getLogger(UserAccountScheduler.class);

    private final UserRepository userRepository;

    private final AppValidityTimeConfig appValidityTimeConfig;

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired every day, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
                .findAllByStatusAndCreatedDateBefore(EntityStatus.PENDING, Instant.now()
                        .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))
                .ifPresent(users -> {
                    userRepository.deleteAll(users);
                    log.info("Successfully Deleted users that are not activated for last {} days: {}",
                            appValidityTimeConfig.getActivationLink(), users);
                });
    }

}
