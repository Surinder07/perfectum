package ca.waaw.repository;

import ca.waaw.domain.OrganizationHolidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationHolidayRepository extends JpaRepository<OrganizationHolidays, String> {

    Optional<OrganizationHolidays> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    /*
     * If month is null whole year data will be sent
     */
    @Query(value = "SELECT h FROM OrganizationHolidays h WHERE h.locationId = ?1 AND " +
            "h.deleteFlag = false AND h.year = ?2")
    List<OrganizationHolidays> getAllForLocationByYear(String locationId, int year);

    /*
     * If month is null whole year data will be sent
     */
    @Query(value = "SELECT h FROM OrganizationHolidays h WHERE h.organizationId = ?1 AND " +
            "h.deleteFlag = false AND h.year = ?2 AND h.locationId IS NULL")
    List<OrganizationHolidays> getAllForOrganizationByYear(String organizationId, int year);

}