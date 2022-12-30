package ca.waaw.repository;

import ca.waaw.domain.LocationRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRoleRepository extends JpaRepository<LocationRole, String> {

    Optional<LocationRole> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    List<LocationRole> findAllByDeleteFlagAndIdIn(boolean deleteFlag, List<String> id);

    @Query(value = "SELECT waaw_id from location_role WHERE waaw_id IS NOT NULL ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    Optional<String>getLastUsedWaawId();

    Optional<LocationRole> getByNameAndLocationId(String name, String locationId);

    Page<LocationRole> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag, Pageable pageable);

    Page<LocationRole> findAllByLocationIdAndDeleteFlagAndAdminRights(String locationId, boolean deleteFlag, boolean adminRights, Pageable pageable);

    List<LocationRole> getListByNameAndLocation(List<String> name, List<String> locationId);

    List<LocationRole> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

}