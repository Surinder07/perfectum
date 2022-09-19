package ca.waaw.repository;

import ca.waaw.domain.OrganizationHolidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationHolidayRepository extends JpaRepository<OrganizationHolidays, String> {

    Optional<OrganizationHolidays> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    /*
     * If month is null whole year data will be sent
     */
    List<OrganizationHolidays> getAllForLocationAndMonthIfNeeded(String locationId, Integer month, int year);

    /*
     * If month is null whole year data will be sent
     */
    List<OrganizationHolidays> getAllForOrganizationAndMonthIfNeeded(String organizationId, Integer month, int year);

}