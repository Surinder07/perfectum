package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.dto.holiday.HolidayDto;
import ca.waaw.dto.userdtos.OrganizationPreferences;
import ca.waaw.dto.userdtos.UserDetailsDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.FileType;
import ca.waaw.enumration.NotificationType;
import ca.waaw.filehandler.FileHandler;
import ca.waaw.filehandler.enumration.PojoToMap;
import ca.waaw.mapper.OrganizationHolidayMapper;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.organization.OrganizationHolidayRepository;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.AppNotificationService;
import ca.waaw.service.WebSocketService;
import ca.waaw.storage.AzureStorage;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.FileNotReadableException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.errors.exceptions.application.FutureCalenderNotAccessibleException;
import ca.waaw.web.rest.errors.exceptions.application.MissingRequiredFieldsException;
import ca.waaw.web.rest.errors.exceptions.application.PastValueNotDeletableException;
import ca.waaw.web.rest.utils.ApiResponseMessageKeys;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.MessageConstants;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final AppNotificationService appNotificationService;

    private final AzureStorage azureStorage;

    private final WebSocketService webSocketService;

    private final AppUrlConfig appUrlConfig;

    private final MessageSource messageSource;

    /**
     * Updates the preferences of logged-in admins organization
     *
     * @param preferences preferences to be updated
     */
    public void updateOrganizationPreferences(OrganizationPreferences preferences) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        String userId = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false))
                .map(organization -> UserMapper.updateOrganizationPreferences(organization, preferences))
                .map(organization -> CommonUtils.logMessageAndReturnObject(organization, "info", UserService.class,
                        "Organization Preferences for organization id ({}) updated: {}", organization.getId(), preferences))
                .map(organizationRepository::save)
                .map(Organization::getCreatedBy)
                .orElseThrow(AuthenticationException::new);
        UserDetailsDto response = userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                .map(UserMapper::entityToDto)
                .map(userDto -> {
                    userDto.setImageUrl(userDto.getImageUrl() == null ? null : appUrlConfig.getImageUrl(userDto.getId(), "profile"));
                    userDto.setOrganizationLogoUrl(userDto.getOrganizationLogoUrl() == null ? null : appUrlConfig.getImageUrl(userDto.getOrganizationId(), "organization"));
                    return userDto;
                })
                .orElseThrow(AuthenticationException::new);
        webSocketService.updateUserDetailsForUi(response.getUsername(), response);
    }

    /**
     * @param file excel or csv file containing holidays
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponseMessageDto uploadHolidaysByExcel(MultipartFile file) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        AtomicReference<String> timezone = new AtomicReference<>();
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(user -> {
                            if (user.getAuthority().equals(Authority.MANAGER)) {
                                timezone.set(user.getLocation().getTimezone());
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
        Set<String> missingFields = new HashSet<>();
        MutableBoolean pastDates = new MutableBoolean(false);
        MutableBoolean nextYearDates = new MutableBoolean(false);
        Set<OrganizationHolidays> holidays = fileHandler.readExcelOrCsv(fileInputStream, fileName,
                        OrganizationHolidays.class, missingFields, PojoToMap.HOLIDAY)
                .parallelStream().peek(holiday -> {
                    holiday.setOrganizationId(admin.getOrganizationId());
                    holiday.setCreatedBy(admin.getId());
                    if (admin.getAuthority().equals(Authority.MANAGER)) {
                        holiday.setLocationId(admin.getLocationId());
                    }
                }).filter(holiday -> {
                    boolean isPastDate = isPastDate(holiday.getYear(), holiday.getMonth(), holiday.getDate(), timezone.get());
                    boolean isNextYearDate = isNextYearDate(holiday.getYear(), holiday.getMonth(), timezone.get());
                    if (isPastDate) pastDates.setTrue();
                    if (isNextYearDate) nextYearDates.setTrue();
                    return !isNextYearDate && !isPastDate;
                })
                .collect(Collectors.toSet());
        if (missingFields.size() > 0) {
            throw new MissingRequiredFieldsException("excel/csv", missingFields.toArray(missingFields.toArray(new String[0])));
        }
        CompletableFuture.runAsync(() -> {
            holidayRepository.saveAll(holidays);
            NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                    .receiverUuid(admin.getId())
                    .receiverName(admin.getFullName())
                    .receiverMail(admin.getEmail())
                    .receiverUsername(admin.getUsername())
                    .receiverMobile(admin.getMobile() == null ? null : admin.getCountryCode() + admin.getMobile())
                    .language(admin.getLangKey() == null ? null : admin.getLangKey())
                    .type(NotificationType.CALENDAR)
                    .build();
            DateTimeDto now = DateAndTimeUtils.getCurrentDateTime(admin.getAuthority().equals(Authority.ADMIN) ?
                    admin.getOrganization().getTimezone() : admin.getLocation().getTimezone());
           appNotificationService.sendApplicationNotification(MessageConstants.holidaysUpload, notificationInfo,
                   false, now.getDate(), now.getTime());
            if (pastDates.isTrue())
               appNotificationService.sendApplicationNotification(MessageConstants.pastHolidays, notificationInfo,
                       false, now.getDate(), now.getTime());
            if (nextYearDates.isTrue())
               appNotificationService.sendApplicationNotification(MessageConstants.futureHolidays, notificationInfo,
                       false, now.getDate(), now.getTime());
            webSocketService.notifyUserAboutHolidayUploadComplete(admin.getUsername());
        });
       return new ApiResponseMessageDto(messageSource.getMessage(ApiResponseMessageKeys.fileUploadProcessing,
               null, new Locale(admin.getLangKey())));
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

    public List<HolidayDto> getAllHolidays(Integer year) {
        UserOrganization userDetails = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        Set<OrganizationHolidays> holidays = null;
        if (StringUtils.isNotEmpty(userDetails.getLocationId())) {
            holidays = new HashSet<>(holidayRepository.getAllForLocationByYear(userDetails.getLocationId(), year));
        }
        if ((holidays != null && holidays.size() < 1) || userDetails.getAuthority().equals(Authority.ADMIN)) {
            holidays = new HashSet<>(holidayRepository.getAllForOrganizationByYear(userDetails.getOrganizationId(), year));
        }
        assert holidays != null;
        return holidays.stream()
                .filter(holiday -> holiday.getType() != null)
                .map(OrganizationHolidayMapper::entityToDto)
                .collect(Collectors.toList());
    }

    public void updateOrganizationLogo(MultipartFile file) throws Exception {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        String fileName;
        try {
            fileName = azureStorage.uploadFile(file, FileType.PICTURES);
        } catch (IOException e) {
            throw new Exception("There was an error while uploading your image.");
        }
        UserOrganization user = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(userOrganization -> {
                    Organization organization = userOrganization.getOrganization();
                    organization.setImageFile(fileName);
                    organization.setLastModifiedBy(organization.getCreatedBy());
                    organizationRepository.save(organization);
                    return userOrganization;
                })
                .orElseThrow(AuthenticationException::new);
        UserDetailsDto response = Optional.of(user)
                .map(UserMapper::entityToDto)
                .map(userDto -> {
                    userDto.setImageUrl(userDto.getImageUrl() == null ? null : appUrlConfig.getImageUrl(userDto.getId(), "profile"));
                    userDto.setOrganizationLogoUrl(userDto.getOrganizationLogoUrl() == null ? null : appUrlConfig.getImageUrl(userDto.getOrganizationId(), "organization"));
                    return userDto;
                })
                .orElseThrow(AuthenticationException::new);
        webSocketService.updateUserDetailsForUi(response.getUsername(), response);
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
        return DateAndTimeUtils.getCurrentDate("year", timezone) > year ||
                DateAndTimeUtils.getCurrentDate("month", timezone) > month ||
                (DateAndTimeUtils.getCurrentDate("month", timezone) == month &&
                        DateAndTimeUtils.getCurrentDate("date", timezone) > date);
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
                (year - DateAndTimeUtils.getCurrentDate("year", timezone)) > 1 ||
                        // Check that year is not a future year unless that month is december
                        (DateAndTimeUtils.getCurrentDate("year", timezone) < year && month != 12);
    }

}