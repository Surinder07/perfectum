package ca.waaw.web.rest.service;

import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.user.User;
import ca.waaw.enumration.FileType;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.storage.AzureStorage;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ResourceService {

    private final Logger log = LogManager.getLogger(ResourceService.class);

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final AzureStorage azureStorage;

    public byte[] getImageLink(String type, String id) {
        String fileName = null;
        if (type.equalsIgnoreCase("profile")) {
            fileName = userRepository.findOneByIdAndDeleteFlag(id, false)
                    .map(User::getImageFile)
                    .orElseThrow(() -> new EntityNotFoundException("user"));
        } else if (type.equalsIgnoreCase("organization")) {
            fileName = organizationRepository.findOneByIdAndDeleteFlag(id, false)
                    .map(Organization::getImageFile)
                    .orElseThrow(() -> new EntityNotFoundException("organization"));
        }
        return azureStorage.retrieveFileData(fileName, FileType.PICTURES);
    }

}