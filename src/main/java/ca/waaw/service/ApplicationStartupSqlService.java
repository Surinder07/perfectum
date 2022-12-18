package ca.waaw.service;

import ca.waaw.WaawApplication;
import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppSuperUserConfig;
import ca.waaw.domain.*;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.Authority;
import ca.waaw.repository.*;
import ca.waaw.web.rest.utils.CommonUtils;
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

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final EmployeePreferencesRepository preferencesRepository;

    private final AppSuperUserConfig appSuperUserConfig;

    private final AppCustomIdConfig appCustomIdConfig;

    private final PasswordEncoder passwordEncoder;

    private final DataSource dataSource;

    private final ResourcePatternResolver resourcePatternResolver;

    private final Environment env;

    /**
     * Will generate a user with {@link Authority#SUPER_USER} authority
     * If a super-user is already present in the database, new user will not be created.
     * If application.create-dummy-data-on-startup is set to true in application-profile.yml, it will
     * also create some dummy users and locations
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkExistenceAndGenerateSuperUser() {
        log.info("Checking and creating a super-user for the application...");
        userRepository.findOneByAuthority(Authority.SUPER_USER)
                .ifPresentOrElse(user -> log.info("A super-user is already present in the database: {}", user),
                        () -> {
                            String currentOrgCustomId = organizationRepository.getLastUsedCustomId()
                                    .orElse(appCustomIdConfig.getOrganizationPrefix() + "000000000");
                            Organization organization = new Organization();
                            organization.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
                            organization.setName(appSuperUserConfig.getOrganization());
                            organization.setTimezone(appSuperUserConfig.getTimezone());
                            organization.setCreatedBy("SYSTEM");
                            organizationRepository.save(organization);
                            log.info("Created a new organization: {}", organization);
                            String currentCustomId = userRepository.getLastUsedCustomId()
                                    .orElse(appCustomIdConfig.getUserPrefix() + "000000000");
                            User superUser = saveNewUser(appSuperUserConfig.getFirstName(), appSuperUserConfig.getLastName(),
                                    appSuperUserConfig.getUsername(), appSuperUserConfig.getEmail(), appSuperUserConfig.getPassword(),
                                    CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()),
                                    Authority.SUPER_USER, organization.getId(), null, null);
                            createDemoUsersAndLocations(organization.getId(), superUser.getWaawId());
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
            log.info("Executing Sql trigger scripts...");
            ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
            String triggerPath = env.getProperty("application.triggers.location");
            Resource[] resources = resourcePatternResolver.getResources(triggerPath + "*.sql");
            resourceDatabasePopulator.addScripts(resources);
            resourceDatabasePopulator.setSeparator("//");
            resourceDatabasePopulator.execute(dataSource);
            log.info("Executing Sql trigger scripts successful.");
        } catch (Exception e) {
            log.error("Executing Sql trigger scripts failed: {}", e.getMessage());
        }
    }

    public void createDemoUsersAndLocations(String organizationId, String currentCustomId) {
        if (Boolean.parseBoolean(env.getProperty("application.create-dummy-data-on-startup"))) {
            // Create an organization admin
            User admin = saveNewUser("Global", "Admin", "gAdmin", "gadmin@waaw.ca", "Admin123$",
                    CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()),
                    Authority.ADMIN, organizationId, null, null);
            // Create a new location
            String locationId = createNewLocation(organizationId, admin.getId());
            // Create a location admin
            User manager = saveNewUser("Location", "Admin", "lAdmin", "ladmin@waaw.ca", "Admin123$",
                    CommonUtils.getNextCustomId(admin.getWaawId(), appCustomIdConfig.getLength()),
                    Authority.MANAGER, organizationId, locationId, null);
            // Create a new location role
            String locationRoleId = createNewLocationRole(organizationId, locationId, manager.getId());
            // Create new Employees
            User employee1 = saveNewUser("First", "Employee", "employee1", "employee1@waaw.ca",
                    "EMPL123$", CommonUtils.getNextCustomId(manager.getWaawId(), appCustomIdConfig.getLength()),
                    Authority.EMPLOYEE, organizationId, locationId, locationRoleId);
            User employee2 = saveNewUser("Second", "Employee", "employee2", "employee2@waaw.ca",
                    "EMPL123$", CommonUtils.getNextCustomId(employee1.getWaawId(), appCustomIdConfig.getLength()),
                    Authority.EMPLOYEE, organizationId, locationId, locationRoleId);
            // Create Employee Preferences
            createEmployeePreferences(employee1.getId());
            createEmployeePreferences(employee2.getId());
        }
    }

    private User saveNewUser(String fName, String lName, String username, String email, String password, String customId,
                               Authority role, String organizationId, String locationId, String locationRoleId) {
        User user = new User();
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setEmail(email);
        user.setUsername(username);
        user.setWaawId(customId);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountStatus(AccountStatus.PAID_AND_ACTIVE);
        user.setCreatedBy("SYSTEM");
        user.setAuthority(role);
        user.setOrganizationId(organizationId);
        user.setLocationId(locationId);
        user.setLocationRoleId(locationRoleId);
        userRepository.save(user);
        log.info("Created a new {}: {}", role, user);
        return user;
    }

    private String createNewLocation(String organizationId, String admin) {
        Location location = new Location();
        location.setName("Test location");
        location.setOrganizationId(organizationId);
        location.setTimezone(appSuperUserConfig.getTimezone());
        location.setCreatedBy(admin);
        location.setWaawId("L0000000001");
        locationRepository.save(location);
        log.info("New Location created: {}", location);
        return location.getId();
    }

    private String createNewLocationRole(String organizationId, String locationId, String admin) {
        LocationRole role = new LocationRole();
        role.setName("Test Role");
        role.setOrganizationId(organizationId);
        role.setLocationId(locationId);
        role.setWaawId("R0000000001");
        role.setCreatedBy(admin);
        role.setAdminRights(true);
        locationRoleRepository.save(role);
        log.info("New location role saved: {}", role);
        return role.getId();
    }

    private void createEmployeePreferences(String userId) {
        EmployeePreferences preferences = new EmployeePreferences();
        preferences.setUserId(userId);
        preferences.setMondayStartTime("09:00");
        preferences.setTuesdayStartTime("10:00");
        preferences.setThursdayStartTime("09:00");
        preferences.setFridayStartTime("10:00");
        preferences.setMondayWorkingHours(getRandomWorkingHours());
        preferences.setTuesdayWorkingHours(getRandomWorkingHours());
        preferences.setThursdayWorkingHours(getRandomWorkingHours());
        preferences.setFridayWorkingHours(getRandomWorkingHours());
        preferences.setCreatedBy("SYSTEM");
        preferencesRepository.save(preferences);
        log.info("Saved new preferences for user {} : {}", userId, preferences);
    }

    private int getRandomWorkingHours() {
        return 4 + (int) (Math.random() * 5);
    }

}
