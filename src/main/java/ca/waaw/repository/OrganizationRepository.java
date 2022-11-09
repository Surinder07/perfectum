package ca.waaw.repository;

import ca.waaw.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<Organization> findOneByIdAndDeleteFlagAndTrialDaysNot(String id, boolean deleteFlag, int trialDaysNot);

    @Query(value = "SELECT num FROM (SELECT ROW_NUMBER() OVER (ORDER BY created_date ASC) AS num, uuid " +
            "AS id FROM organization WHERE SUBSTRING(name, 1, 3) = SUBSTRING(?1, 1, 3) ) AS TEMP " +
            "WHERE id = ?2", nativeQuery = true)
    int getShiftBatchPrefixByOrganization(String organizationName, String organizationId);

    @Query(value = "SELECT name from shifts_batch WHERE name IS NOT NULL ORDER BY created_date DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

}