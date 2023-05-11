package ca.waaw.service;

import ca.waaw.WaawApplication;
import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppSuperUserConfig;
import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.payments.PromotionCode;
import ca.waaw.domain.user.EmployeePreferences;
import ca.waaw.domain.user.User;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.Currency;
import ca.waaw.enumration.payment.PromoCodeType;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.locationandroles.LocationRoleRepository;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.payments.PromotionCodeRepository;
import ca.waaw.repository.user.EmployeePreferencesRepository;
import ca.waaw.repository.user.UserRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final EmployeePreferencesRepository preferencesRepository;

    private final PromotionCodeRepository promotionCodeRepository;

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
     * also create some dummy users, locations and location roles
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkExistenceAndGenerateSuperUser() {
        log.info("Checking and creating a super-user for the application...");
        userRepository.findOneByAuthority(Authority.SUPER_USER)
                .ifPresentOrElse(user -> log.info("A super-user is already present in the database: {}", user),
                        () -> {
                            String userId = UUID.randomUUID().toString();
                            String organizationId = createNewOrganization(null, userId);
                            String currentCustomId = userRepository.getLastUsedCustomId()
                                    .orElse(appCustomIdConfig.getUserPrefix() + "000000000");
                            User superUser = saveNewUser(appSuperUserConfig.getFirstName(), appSuperUserConfig.getLastName(),
                                    appSuperUserConfig.getUsername(), appSuperUserConfig.getEmail(), appSuperUserConfig.getPassword(),
                                    CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()),
                                    Authority.SUPER_USER, organizationId, null, null, userId);
                            createDemoUsersAndLocations(superUser.getWaawId());
                        }
                );
    }

    public void checkExistenceAndGeneratePromoCodes() {
        log.info("Generating promo codes for 1 day, 10 days, 20 days and 30 days.");
        if (promotionCodeRepository.findAll().size() == 0) {
            List<PromotionCode> codes = List.of(
                    PromotionCode.builder().code("WAAW01").promotionValue(1).type(PromoCodeType.TRIAL).build(),
                    PromotionCode.builder().code("WAAW10").promotionValue(10).type(PromoCodeType.TRIAL).build(),
                    PromotionCode.builder().code("WAAW20").promotionValue(20).type(PromoCodeType.TRIAL).build(),
                    PromotionCode.builder().code("WAAW30").promotionValue(30).type(PromoCodeType.TRIAL).build()
            );
            promotionCodeRepository.saveAll(codes);
            log.info("Generating promo codes for 1 day, 10 days, 20 days and 30 days successful.\n {}", codes);
        } else {
            log.info("Skipped generating promo codes as some already exists.");
        }
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

    public void createDemoUsersAndLocations(String currentCustomId) {
        if (Boolean.parseBoolean(env.getProperty("application.create-dummy-data-on-startup"))) {
            String userId = UUID.randomUUID().toString();
            String organizationId = createNewOrganization("WAAW TEST", userId);
            // Create an organization admin
            User admin = saveNewUser("Global", "Admin", "gAdmin", "gadmin@waaw.ca", "Admin123$",
                    CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()),
                    Authority.ADMIN, organizationId, null, null, userId);
            // Create a new location
            String locationId = createNewLocation(organizationId, admin.getId());
            // Create a new location role
            String locationRoleAdminId = createNewLocationRole(organizationId, locationId, admin.getId(), true);
            // Create a location admin
            User manager = saveNewUser("Location", "Admin", "lAdmin", "ladmin@waaw.ca", "Admin123$",
                    CommonUtils.getNextCustomId(admin.getWaawId(), appCustomIdConfig.getLength()),
                    Authority.MANAGER, organizationId, locationId, locationRoleAdminId, null);
            String locationRoleId = createNewLocationRole(organizationId, locationId, manager.getId(), false);
            // Create new Employees
            User employee1 = saveNewUser("First", "Employee", "employee1", "employee1@waaw.ca",
                    "Empl123$", CommonUtils.getNextCustomId(manager.getWaawId(), appCustomIdConfig.getLength()),
                    Authority.EMPLOYEE, organizationId, locationId, locationRoleId, null);
            User employee2 = saveNewUser("Second", "Employee", "employee2", "employee2@waaw.ca",
                    "Empl123$", CommonUtils.getNextCustomId(employee1.getWaawId(), appCustomIdConfig.getLength()),
                    Authority.EMPLOYEE, organizationId, locationId, locationRoleId, null);
            // Create Employee Preferences
            createEmployeePreferences(manager.getId());
            createEmployeePreferences(employee1.getId());
            createEmployeePreferences(employee2.getId());
        }
    }

    private String createNewOrganization(String name, String userId) {
        String currentOrgCustomId = organizationRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getOrganizationPrefix() + "000000000");
        Organization organization = new Organization();
        organization.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
        organization.setName(name == null ? appSuperUserConfig.getOrganization() : name);
        organization.setTimezone(appSuperUserConfig.getTimezone());
        organization.setTrialEndDate(Instant.now());
        organization.setNextPaymentOn(Instant.now().plus(300, ChronoUnit.DAYS));
        organization.setPlatformFeePaid(true);
        organization.setCreatedBy(userId);
        organizationRepository.save(organization);
        log.info("Created a new organization: {}", organization);
        return organization.getId();
    }

    private User saveNewUser(String fName, String lName, String username, String email, String password, String customId,
                             Authority role, String organizationId, String locationId, String locationRoleId, String userId) {
        User user = new User();
        if (userId != null) user.setId(userId);
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setEmail(email);
        user.setUsername(username);
        user.setWaawId(customId);
        if (role.equals(Authority.ADMIN)) user.setStripeId("cus_NpW3H7xDkEi1Vi");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountStatus((role.equals(Authority.SUPER_USER) || role.equals(Authority.ADMIN)) ?
                AccountStatus.PAID_AND_ACTIVE : AccountStatus.DISABLED);
        user.setCreatedBy("SYSTEM");
        user.setAuthority(role);
        user.setFullTime(true);
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
        location.setActive(false);
        locationRepository.save(location);
        log.info("New Location created: {}", location);
        return location.getId();
    }

    private String createNewLocationRole(String organizationId, String locationId, String admin, boolean adminRights) {
        LocationRole role = new LocationRole();
        role.setName(adminRights ? "Manager" : "Server");
        role.setOrganizationId(organizationId);
        role.setLocationId(locationId);
        role.setWaawId(adminRights ? "R0000000001" : "R0000000002");
        role.setCreatedBy(admin);
        role.setAdminRights(adminRights);
        role.setActive(false);
        locationRoleRepository.save(role);
        log.info("New location role saved: {}", role);
        return role.getId();
    }

    /**
     * Create employee preferences for a dummy user
     *
     * @param userId userId for the employee
     */
    private void createEmployeePreferences(String userId) {
        EmployeePreferences preferences = new EmployeePreferences();
        preferences.setUserId(userId);
        preferences.setMondayStartTime("09:00");
        preferences.setTuesdayStartTime("10:00");
        preferences.setWednesdayStartTime("10:00");
        preferences.setThursdayStartTime("09:00");
        preferences.setFridayStartTime("10:00");
        preferences.setMondayEndTime("17:00");
        preferences.setTuesdayEndTime("17:00");
        preferences.setWednesdayEndTime("18:00");
        preferences.setThursdayEndTime("17:00");
        preferences.setFridayEndTime("17:00");
        preferences.setWagesPerHour(20F);
        preferences.setWagesCurrency(Currency.CAD);
        preferences.setCreatedBy("SYSTEM");
        preferencesRepository.save(preferences);
        log.info("Saved new preferences for user {} : {}", userId, preferences);
    }

}
