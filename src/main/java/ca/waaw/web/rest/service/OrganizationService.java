package ca.waaw.web.rest.service;

import ca.waaw.domain.Location;
import ca.waaw.domain.OrganizationHolidays;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.holiday.HolidayAdminDto;
import ca.waaw.dto.holiday.HolidayDto;
import ca.waaw.dto.userdtos.OrganizationPreferences;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.filehandler.FileHandler;
import ca.waaw.filehandler.enumration.PojoToMap;
import ca.waaw.mapper.OrganizationHolidayMapper;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.*;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.FileNotReadableException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.FutureCalenderNotAccessibleException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.utils.ApiResponseMessageKeys;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrganizationService {

    private final Logger log = LogManager.getLogger(OrganizationService.class);

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final OrganizationHolidayRepository holidayRepository;

    private final LocationRepository locationRepository;

    private final FileHandler fileHandler;

    /**
     * Updates the preferences of logged-in admins organization
     *
     * @param preferences preferences to be updated
     */
    public void updateOrganizationPreferences(OrganizationPreferences preferences) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false))
                .map(organization -> UserMapper.updateOrganizationPreferences(organization, preferences))
                .map(organization -> CommonUtils.logMessageAndReturnObject(organization, "info", UserService.class,
                        "Organization Preferences for organization id ({}) updated: {}", organization.getId(), preferences))
                .map(organizationRepository::save);
    }

    /**
     * @param file       excel or csv file containing holidays
     * @param locationId if holidays are for a particular location, id is required
     */
    public ApiResponseMessageDto uploadHolidaysByExcel(MultipartFile file, String locationId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        AtomicReference<String> timezone = new AtomicReference<>();
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(user -> {
                            if (StringUtils.isNotEmpty(locationId)) {
                                locationRepository.findOneByIdAndDeleteFlag(locationId, false)
                                        .map(location -> {
                                            if (!location.getOrganizationId().equals(user.getLocationId())) {
                                                throw new UnauthorizedException();
                                            }
                                            timezone.set(location.getTimezone());
                                            return location;
                                        })
                                        .orElseThrow(() -> new EntityNotFoundException("location"));
                            } else timezone.set(user.getOrganization().getTimezone());
                            return user;
                        })
                ).orElseThrow(UnauthorizedException::new);
        // Converting file to Input Stream so that it is available in the async process below
        InputStream fileInputStream;
        String fileName;
        try {
            fileInputStream = file.getInputStream();
            fileName = file.getOriginalFilename();
        } catch (IOException e) {
            log.error("Exception while reading file.", e);
            throw new FileNotReadableException();
        }
        CompletableFuture.runAsync(() -> {
            MutableBoolean missingData = new MutableBoolean(false);
            MutableBoolean pastDates = new MutableBoolean(false);
            MutableBoolean nextYearDates = new MutableBoolean(false);
            try {
                List<OrganizationHolidays> holidays = fileHandler.readExcelOrCsv(fileInputStream, fileName,
                                OrganizationHolidays.class, missingData, PojoToMap.HOLIDAY)
                        .parallelStream().peek(holiday -> {
                            holiday.setId(UUID.randomUUID().toString());
                            holiday.setOrganizationId(admin.getOrganizationId());
                            holiday.setCreatedBy(admin.getId());
                            holiday.setStatus(EntityStatus.ACTIVE);
                            if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) || StringUtils.isNotEmpty(locationId)) {
                                holiday.setLocationId(SecurityUtils.isCurrentUserInRole(Authority.MANAGER) ?
                                        admin.getLocationId() : locationId);
                            }
                        }).filter(holiday -> {
                            boolean isPastDate = isPastDate(holiday.getYear(), holiday.getMonth(), holiday.getDate(), timezone.get());
                            boolean isNextYearDate = isNextYearDate(holiday.getYear(), holiday.getMonth(), timezone.get());
                            if (isPastDate) pastDates.setTrue();
                            if (isNextYearDate) nextYearDates.setTrue();
                            return !isNextYearDate && !isPastDate;
                        })
                        .collect(Collectors.toList());
                holidayRepository.saveAll(holidays);
                // TODO Send notification including on missingData, pastDate & nextYearDate
            } catch (Exception e) {
                // TODO Send the failure notification
            }
        });
        return new ApiResponseMessageDto(CommonUtils.getPropertyFromMessagesResourceBundle(ApiResponseMessageKeys
                .fileUploadProcessing, new Locale(admin.getLangKey())));
    }

    /**
     * @param holidayDto Holiday details to be added
     */
    public void addHoliday(HolidayDto holidayDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                            AtomicReference<String> timezone = new AtomicReference<>();
                            if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER)) {
                                holidayDto.setLocationId(user.getLocationId());
                            }
                            if (StringUtils.isNotEmpty(holidayDto.getLocationId())) {
                                locationRepository.findOneByIdAndDeleteFlag(holidayDto.getLocationId(), false)
                                        .map(location -> {
                                            if (!user.getOrganizationId().equals(location.getOrganizationId())) {
                                                throw new UnauthorizedException();
                                            }
                                            timezone.set(location.getTimezone());
                                            return location;
                                        });
                            } else {
                                timezone.set(user.getOrganization().getTimezone());
                            }
                            validateDate(holidayDto, timezone.get());
                            OrganizationHolidays holiday = OrganizationHolidayMapper.newDtoToEntity(holidayDto);
                            holiday.setOrganizationId(user.getOrganizationId());
                            holiday.setCreatedBy(user.getId());
                            return holiday;
                        }
                )
                .map(holidayRepository::save)
                .map(holiday -> CommonUtils.logMessageAndReturnObject(OrganizationHolidays.class, "info",
                        OrganizationService.class, "New Holiday added: {}", holiday));
    }

    /**
     * @param holidayDto Holiday info to be updated
     */
    public void editHoliday(HolidayDto holidayDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(user -> holidayRepository.findOneByIdAndDeleteFlag(holidayDto.getId(), false)
                                .map(holiday -> {
                                    if (!holiday.getOrganizationId().equals(user.getOrganizationId())) {
                                        throw new UnauthorizedException();
                                    }
                                    AtomicReference<String> timezone = new AtomicReference<>("");
                                    if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) &&
                                            !user.getLocationId().equals(holiday.getLocationId())) {
                                        throw new UnauthorizedException();
                                    }
                                    // If locationId is being changed check admin authorization
                                    if (StringUtils.isNotEmpty(holidayDto.getLocationId()) && (StringUtils.isEmpty(holiday.getLocationId())) ||
                                            !holidayDto.getLocationId().equals(holiday.getLocationId())) {
                                        locationRepository.findOneByIdAndDeleteFlag(holidayDto.getLocationId(), false)
                                                .map(location -> {
                                                    timezone.set(location.getTimezone());
                                                    if (!location.getOrganizationId().equals(user.getOrganizationId())) {
                                                        throw new UnauthorizedException();
                                                    }
                                                    return location;
                                                })
                                                .orElseThrow(() -> new EntityNotFoundException("location"));
                                    } else {
                                        timezone.set(user.getOrganization().getTimezone());
                                    }
                                    validateDate(holidayDto, timezone.get());
                                    return holiday;
                                })
                                .map(holiday -> {
                                    OrganizationHolidayMapper.updateDtoToEntity(holidayDto, holiday);
                                    holiday.setLastModifiedBy(user.getId());
                                    return holiday;
                                })
                                .orElseThrow(() -> new EntityNotFoundException("holiday"))
                        )
                        .map(holidayRepository::save)
                        .map(holiday -> CommonUtils.logMessageAndReturnObject(OrganizationHolidays.class, "info",
                                OrganizationService.class, "Holiday updated: {}", holiday))
                );
    }

    /**
     * @param holidayId id for holiday to be deleted
     */
    public void deleteHoliday(String holidayId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> holidayRepository.findOneByIdAndDeleteFlag(holidayId, false)
                        .map(holiday -> {
                            if (!holiday.getOrganizationId().equals(user.getOrganizationId())) {
                                throw new UnauthorizedException();
                            }
                            if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER) && !holiday.getLocationId().equals(user.getLocationId())) {
                                throw new UnauthorizedException();
                            }
                            holiday.setDeleteFlag(true);
                            holiday.setLastModifiedBy(user.getId());
                            return holidayRepository.save(holiday);
                        })
                        .map(holiday -> CommonUtils.logMessageAndReturnObject(OrganizationHolidays.class, "info",
                                OrganizationService.class, "Holiday deleted successfully: {}", holiday))
                        .orElseThrow(() -> new EntityNotFoundException("holiday"))
                );

    }

    public List<?> getAllHolidays(Integer month) {
        UserOrganization userDetails = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ? userDetails.getOrganization().getTimezone()
                : userDetails.getLocation().getTimezone();
        int currentYear = DateUtils.getCurrentDate("year", timezone);
        List<OrganizationHolidays> holidays;
        if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN)) {
            holidays = holidayRepository.getAllForOrganizationAndMonthIfNeeded(userDetails.getOrganizationId(), month, currentYear);
        } else {
            holidays = holidayRepository.getAllForLocationAndMonthIfNeeded(userDetails.getLocationId(), month, currentYear);
        }
        return populateHolidaysBasedOnRole(holidays, SecurityUtils.isCurrentUserInRole(Authority.ADMIN));
    }

    /**
     * Will throw an error if date is in past or next year (next year is allowed if month is december)
     *
     * @param holidayDto holiday details to be added
     * @param timezone   timezone for the location or organization for which holiday is being created
     */
    private void validateDate(HolidayDto holidayDto, String timezone) {
        if (isPastDate(holidayDto.getYear(), holidayDto.getMonth(), holidayDto.getDate(), timezone)) {
            throw new PastValueNotDeletableException("holiday");
        } else if (isNextYearDate(holidayDto.getYear(), holidayDto.getMonth(), timezone)) {
            throw new FutureCalenderNotAccessibleException();
        }
    }

    /**
     * @param year     year from dto
     * @param month    month from dto
     * @param date     date from dto
     * @param timezone timezone for the location or organization for which holiday is being created
     * @return true if date is in past
     */
    private boolean isPastDate(int year, int month, int date, String timezone) {
        return DateUtils.getCurrentDate("year", timezone) > year ||
                DateUtils.getCurrentDate("month", timezone) > month ||
                (DateUtils.getCurrentDate("month", timezone) == month &&
                        DateUtils.getCurrentDate("date", timezone) > date);
    }

    /**
     * @param year     year from dto
     * @param month    month from dto
     * @param timezone timezone for the location or organization for which holiday is being created
     * @return true if date is in next year while month is not december
     */
    private boolean isNextYearDate(int year, int month, String timezone) {
        return
                // check that year is not more than one year ahead
                (year - DateUtils.getCurrentDate("year", timezone)) > 1 ||
                        // Check that year is not a future year unless that month is december
                        (DateUtils.getCurrentDate("year", timezone) < year && month != 12);
    }

    /**
     * @param holidays list of holidays to be returned to frontend
     * @param isAdmin  if logged-in user is global admin
     * @return list of holidayDto if user is not global admin and list of holidayDto mapped with locations if user is global admin
     */
    private List<?> populateHolidaysBasedOnRole(List<OrganizationHolidays> holidays, boolean isAdmin) {
        List<HolidayDto> holidaysResponse = holidays.stream().map(OrganizationHolidayMapper::entityToDto)
                .collect(Collectors.toList());
        if (isAdmin) {
            List<String> locationIds = holidaysResponse.stream()
                    .map(HolidayDto::getLocationId)
                    .filter(Objects::nonNull)
                    .distinct().collect(Collectors.toList());
            Map<String, String> locationNameMap = locationRepository.findAllByIdIn(locationIds)
                    .stream().collect(Collectors.toMap(Location::getId, Location::getName));
            List<HolidayAdminDto> adminResponse = new ArrayList<>();
            locationIds.forEach(locationId -> {
                HolidayAdminDto adminDto = new HolidayAdminDto();
                adminDto.setLocationId(locationId);
                adminDto.setLocationName(locationNameMap.get(locationId));
                adminDto.setHolidays(holidaysResponse.stream()
                        .filter(holiday -> holiday.getLocationId().equals(locationId))
                        .collect(Collectors.toList())
                );
                adminResponse.add(adminDto);
            });
            HolidayAdminDto adminDto = new HolidayAdminDto();
            adminDto.setHolidays(holidaysResponse.stream()
                    .filter(holiday -> StringUtils.isEmpty(holiday.getLocationId()))
                    .collect(Collectors.toList())
            );
            if (adminDto.getHolidays().size() > 0) adminResponse.add(adminDto);
            return adminResponse;
        }
        return holidaysResponse;
    }

}