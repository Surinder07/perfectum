package ca.waaw.service;

import ca.waaw.repository.organization.OrganizationRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CachingService {

    @Lazy
    private final OrganizationRepository organizationRepository;

    @Cacheable(value = "batchNamePrefix", key = "#organizationId")
    public int getOrganizationPrefix(String organizationId, String organizationName) {
        return organizationRepository.getShiftBatchPrefixByOrganization(organizationName, organizationId);
    }

}