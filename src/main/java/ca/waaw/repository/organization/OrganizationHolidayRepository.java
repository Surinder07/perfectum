package ca.waaw.repository.organization;

import ca.waaw.domain.organization.OrganizationHolidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationHolidayRepository extends JpaRepository<OrganizationHolidays, String> {

    Optional<OrganizationHolidays> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = "SELECT h FROM OrganizationHolidays h WHERE (?1 IS NULL OR h.organizationId = ?1) AND " +
            "(?2 IS NULL OR h.locationId = ?2) AND h.year = ?3 AND h.deleteFlag = false")
    List<OrganizationHolidays> getAllForOrganizationOrLocationByYear(String organizationId, String locationId, int year);

    @Query(value = "SELECT h FROM OrganizationHolidays h WHERE h.locationId = ?1 AND " +
            "h.deleteFlag = false AND h.year = ?2")
    List<OrganizationHolidays> getAllForLocationByYear(String locationId, int year);

    @Query(value = "SELECT h FROM OrganizationHolidays h WHERE h.organizationId = ?1 AND " +
            "h.deleteFlag = false AND h.year = ?2 AND h.locationId IS NULL")
    List<OrganizationHolidays> getAllForOrganizationByYear(String organizationId, int year);

}