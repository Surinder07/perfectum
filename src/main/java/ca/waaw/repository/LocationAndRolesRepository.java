package ca.waaw.repository;

import ca.waaw.domain.joined.LocationAndRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationAndRolesRepository extends JpaRepository<LocationAndRoles, String> {
    Optional<LocationAndRoles> findOneByIdAndDeleteFlag(String locationId, boolean deleteFlag);
    List<LocationAndRoles> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);
}
