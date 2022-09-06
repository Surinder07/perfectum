package ca.waaw.repository;

import ca.waaw.domain.OrganizationHolidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationHolidayRepository extends JpaRepository<OrganizationHolidays, String> {

    List<OrganizationHolidays> getAllForLocation(String locationId);

    List<OrganizationHolidays> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);

}