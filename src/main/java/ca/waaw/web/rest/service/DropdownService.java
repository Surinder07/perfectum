package ca.waaw.web.rest.service;

import ca.waaw.enumration.Currency;
import ca.waaw.enumration.*;
import ca.waaw.repository.LocationRepository;
import ca.waaw.repository.LocationRoleRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.repository.UserRepository;
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
        populateListToEnumMap(enumMap, TimeOffType.class);
        populateListToEnumMap(enumMap, TimeSheetType.class);
        return enumMap;
    }

    public List<Map<String, String>> getAllLocations() {
        return SecurityUtils.getCurrentUserLogin()
                        .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                                .map(admin -> locationRepository.findAllByOrganizationIdAndDeleteFlag(admin.getOrganizationId(), false)
                                                .stream()
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
                .flatMap(admin -> locationRepository.findOneByIdAndDeleteFlag(locationId, false)
                        .map(location -> {
                            if (admin.getAuthority().equals(Authority.ADMIN) &&
                                    !location.getOrganizationId().equals(admin.getOrganizationId())) {
                                return null;
                            }
                            return locationRoleRepository.findAllByLocationIdAndDeleteFlag(admin.getAuthority().equals(Authority.ADMIN) ?
                                    locationId : admin.getLocationId(), false)
                                    .stream()
                                    .filter(locationRole -> {
                                        if (admin.getAuthority().equals(Authority.MANAGER)) {
                                            return !locationRole.isAdminRights();
                                        } return true;
                                    })
                                    .map(locationRole -> {
                                        Map<String, String> response = new HashMap<>();
                                        response.put("id", locationRole.getId());
                                        response.put("name", locationRole.getName());
                                        return response;
                                    })
                                    .collect(Collectors.toList());
                        })
                )
                .orElseThrow(() -> new EntityNotFoundException("location"));
    }

    public List<Map<String, String>> getAllUsers() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> userOrganizationRepository.findAllByOrganizationIdAndDeleteFlag(admin.getOrganizationId(), false)
                        .stream()
                        .filter(user -> {
                            if (admin.getAuthority().equals(Authority.ADMIN)) {
                                return !user.getId().equals(admin.getId());
                            } else {
                                return user.getLocationId().equals(admin.getLocationId()) &&
                                        !user.getLocationRole().isAdminRights();
                            }
                        })
                        .map(user -> {
                            Map<String, String> response = new HashMap<>();
                            response.put("id", user.getId());
                            response.put("name", user.getFullName());
                            return response;
                        })
                        .collect(Collectors.toList())
                )
                .orElseThrow(() -> new EntityNotFoundException("locations"));
    }

    private static void populateListToEnumMap(Map<String, List<String>> map, Class<? extends Enum<?>> enumClass) {
        List<?> valuesToIgnore = List.of(new Object[]{Authority.SUPER_USER, Authority.ANONYMOUS});
        map.put(enumClass.getSimpleName(), Stream.of(enumClass.getEnumConstants())
                .filter(value -> !valuesToIgnore.contains(value))
                .map(Objects::toString).collect(Collectors.toList()));
    }

}