package ca.waaw.repository.organization;

import ca.waaw.domain.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = "SELECT o FROM Organization o WHERE o.deleteFlag = FALSE AND o.paymentPending = FALSE")
    List<Organization> getAllActiveOrganization();

    @Query(value = "SELECT o FROM Organization o WHERE o.trialEndDate > CURRENT_TIMESTAMP AND o.deleteFlag = FALSE")
    List<Organization> getAllTrialEnding();

    @Query(value = "SELECT num FROM (SELECT ROW_NUMBER() OVER (ORDER BY created_date ASC) AS num, uuid " +
            "AS id FROM organization WHERE SUBSTRING(name, 1, 3) = SUBSTRING(?1, 1, 3) ) AS TEMP " +
            "WHERE id = ?2", nativeQuery = true)
    int getShiftBatchPrefixByOrganization(String organizationName, String organizationId);

    @Query(value = "SELECT waaw_custom_id from organization WHERE waaw_custom_id IS NOT NULL ORDER BY waaw_custom_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

}