package ca.waaw.web.rest.service;

import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.enumration.Currency;
import ca.waaw.enumration.*;
import ca.waaw.enumration.payment.PromoCodeType;
import ca.waaw.enumration.report.PayrollGenerationType;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.shift.ShiftType;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.locationandroles.LocationRoleRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.ForDevelopmentOnlyException;
import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class DropdownService {

    private Environment environment;

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    public List<String> getAllTimezones() {
        return Arrays.stream(Timezones.values()).map(zone -> zone.value)
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getAllEnums() {
        if (!Boolean.parseBoolean(environment.getProperty("springdoc.swagger-ui.enabled"))) {
            throw new ForDevelopmentOnlyException();
        }
        Map<String, List<String>> enumMap = new HashMap<>();
        populateListToEnumMap(enumMap, Authority.class);
        populateListToEnumMap(enumMap, HolidayType.class);
        populateListToEnumMap(enumMap, NotificationType.class);
        populateListToEnumMap(enumMap, PromoCodeType.class);
        populateListToEnumMap(enumMap, ShiftStatus.class);
        populateListToEnumMap(enumMap, ShiftType.class);
        populateListToEnumMap(enumMap, PayrollGenerationType.class);
        populateListToEnumMap(enumMap, Currency.class);
        populateListToEnumMap(enumMap, DaysOfWeek.class);
        populateListToEnumMap(enumMap, TimeSheetType.class);
        return enumMap;
    }

    public List<Map<String, String>> getAllLocations() {
        return SecurityUtils.getCurrentUserLogin()
                        .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                                .map(admin -> locationRepository.findAllByOrganizationIdAndDeleteFlag(admin.getOrganizationId(), false)
                                                .stream()
                                                .filter(Location::isActive)
                                                .map(location -> {
                                                    Map<String, String> response = new HashMap<>();
                                                    response.put("id", location.getId());
                                                    response.put("name", location.getName());
                                                    return response;
                                                })
                                                .collect(Collectors.toList())
                                        )
                .orElseThrow(() -> new EntityNotFoundException("locations"));
    }

    public List<Map<String, String>> getAllLocationRoles(String locationId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(admin -> {
                    String locId = admin.getAuthority().equals(Authority.MANAGER) ? admin.getLocationId() : locationId;
                    return locationRepository.findOneByIdAndDeleteFlag(locId, false)
                                    .map(location -> {
                                        if (admin.getAuthority().equals(Authority.ADMIN) &&
                                                !location.getOrganizationId().equals(admin.getOrganizationId())) {
                                            return null;
                                        }
                                        return locationRoleRepository.findAllByLocationIdAndDeleteFlag(locId, false)
                                                .stream()
                                                .filter(locationRole -> {
                                                    if (admin.getAuthority().equals(Authority.MANAGER)) {
                                                        return !locationRole.isAdminRights();
                                                    } return true;
                                                })
                                                .filter(LocationRole::isActive)
                                                .map(locationRole -> {
                                                    Map<String, String> response = new HashMap<>();
                                                    response.put("id", locationRole.getId());
                                                    response.put("name", locationRole.getName());
                                                    return response;
                                                })
                                                .collect(Collectors.toList());
                                    });
                        })
                .orElseThrow(() -> new EntityNotFoundException("location"));
    }

    public List<Map<String, String>> getAllUsers() {
        try {
            return SecurityUtils.getCurrentUserLogin()
                    .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                    .map(admin -> userOrganizationRepository.findAllByOrganizationIdAndDeleteFlag(admin.getOrganizationId(), false)
                            .stream()
                            .filter(user -> {
                                if (admin.getAuthority().equals(Authority.ADMIN)) {
                                    return !user.getId().equals(admin.getId());
                                } else {
                                    return user.getLocation() != null &&
                                            user.getLocationId().equals(admin.getLocationId()) &&
                                            !user.getLocationRole().isAdminRights();
                                }
                            })
                            .filter(user -> !user.getAccountStatus().equals(AccountStatus.DISABLED) &&
                                    !user.getAccountStatus().equals(AccountStatus.INVITED))
                            .map(user -> {
                                Map<String, String> response = new HashMap<>();
                                response.put("id", user.getId());
                                response.put("name", user.getFullName());
                                return response;
                            })
                            .collect(Collectors.toList())
                    )
                    .orElseThrow(() -> new EntityNotFoundException("locations"));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void populateListToEnumMap(Map<String, List<String>> map, Class<? extends Enum<?>> enumClass) {
        List<?> valuesToIgnore = List.of(new Object[]{Authority.SUPER_USER, Authority.ANONYMOUS});
        map.put(enumClass.getSimpleName(), Stream.of(enumClass.getEnumConstants())
                .filter(value -> !valuesToIgnore.contains(value))
                .map(Objects::toString).collect(Collectors.toList()));
    }

}