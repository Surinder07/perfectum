package ca.waaw.web.rest.service;

import ca.waaw.dto.HolidayDto;
import ca.waaw.dto.userdtos.OrganizationPreferences;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class OrganizationService {

    private final Logger log = LogManager.getLogger(OrganizationService.class);

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

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

    public Object getAllHolidays() {
        return null;
    }

    public void uploadHolidaysByExcel(MultipartFile file, String locationId) {

    }

    public void addHoliday(HolidayDto holidayDto) {

    }

    public void editHoliday(HolidayDto holidayDto) {

    }

    public void deleteHoliday(String holidayId) {

    }

}