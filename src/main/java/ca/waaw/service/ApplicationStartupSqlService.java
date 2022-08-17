package ca.waaw.service;

import ca.waaw.WaawApplication;
import ca.waaw.config.applicationconfig.AppSuperUserConfig;
import ca.waaw.domain.Organization;
import ca.waaw.domain.User;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.enumration.SubscriptionPlans;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * This service is used in {@link WaawApplication} for the initialization of needed entities or triggers
 * in database
 */
@Service
@AllArgsConstructor
public class ApplicationStartupSqlService {

    private final Logger log = LogManager.getLogger(ApplicationStartupSqlService.class);

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final AppSuperUserConfig appSuperUserConfig;

    private final PasswordEncoder passwordEncoder;

    private final DataSource dataSource;

    private final ResourcePatternResolver resourcePatternResolver;

    private final Environment env;

    /**
     * Will generate a user with {@link Authority#SUPER_USER} authority
     * If a super-user is already present in the database, new user will not be created.
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkExistenceAndGenerateSuperUser() {
        log.info("Checking and creating a super-user for the application...");
        userRepository.findOneByAuthority(Authority.SUPER_USER)
                .ifPresentOrElse(user -> log.info("A super-user is already present in the database: {}", user),
                        () -> {
                            Organization organization = new Organization();
                            organization.setId(UUID.randomUUID().toString());
                            organization.setName(appSuperUserConfig.getOrganization());
                            organization.setSubscriptionPlan(SubscriptionPlans.UNLIMITED);
                            organization.setTrialUsed(false);
                            organization.setStatus(EntityStatus.ACTIVE);
                            organization.setCreatedBy("SYSTEM");
                            organizationRepository.save(organization);
                            User user = new User();
                            user.setId(UUID.randomUUID().toString());
                            user.setFirstName(appSuperUserConfig.getFirstName());
                            user.setLastName(appSuperUserConfig.getLastName());
                            user.setEmail(appSuperUserConfig.getEmail());
                            user.setUsername(appSuperUserConfig.getUsername());
                            user.setPasswordHash(passwordEncoder.encode(appSuperUserConfig.getPassword()));
                            user.setStatus(EntityStatus.ACTIVE);
                            user.setCreatedBy("SYSTEM");
                            user.setAuthority(Authority.SUPER_USER);
                            user.setOrganizationId(organization.getId());
                            userRepository.save(user);
                            log.info("Created a new organization: {}", organization);
                            log.info("Created a new super-user: {}", user);
                        }
                );

    }

    /**
     * SQL Triggers are loaded from {@code db/sqltriggers} folder inside resources, make sure all trigger files are
     * included in `application.trigger.files` property in {@code application.yml}
     * Triggers will be executed only if liquibase is enabled.
     */
    public void createSqlTriggers() {
        try {
            if (Boolean.parseBoolean(env.getProperty("spring.liquibase.enabled"))) {
                log.info("Executing Sql trigger scripts...");
                ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
                String triggerPath = env.getProperty("application.triggers.location");
                Resource[] resources = resourcePatternResolver.getResources(triggerPath + "*.sql");
                resourceDatabasePopulator.addScripts(resources);
                resourceDatabasePopulator.setSeparator("//");
                resourceDatabasePopulator.execute(dataSource);
                log.info("Executing Sql trigger scripts successful.");
            }
        } catch (Exception e) {
            log.error("Executing Sql trigger scripts failed: {}", e.getMessage());
        }
    }

}
