package ca.waaw.repository.locationandroles;

import ca.waaw.domain.locationandroles.LocationRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRoleRepository extends JpaRepository<LocationRole, String> {

    Optional<LocationRole> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    List<LocationRole> findAllByDeleteFlagAndIdIn(boolean deleteFlag, List<String> id);

    @Query(value = "SELECT waaw_id from location_role WHERE waaw_id IS NOT NULL ORDER BY waaw_id DESC LIMIT 1", nativeQuery = true)
    Optional<String>getLastUsedWaawId();

    Optional<LocationRole> getByNameAndLocationId(String name, String locationId);

    @Query(value = "SELECT r FROM LocationRole r WHERE (?1 IS NULL OR r.name LIKE CONCAT('%', ?1, '%')) AND " +
            "(?2 IS NULL OR r.organizationId = ?2) AND (?3 IS NULL OR r.locationId = ?3) AND ((?4 IS NULL OR " +
            "?5 IS NULL) OR (r.createdDate BETWEEN ?4 AND ?5)) AND (?6 IS NULL OR r.adminRights = ?6) AND " +
            "(?7 IS NULL OR r.isActive = ?7) AND r.deleteFlag = FALSE")
    Page<LocationRole> searchAndFilterRole(String searchKey, String organizationId, String locationId, Instant startDate,
                                           Instant endDate, Boolean isAdmin, Boolean active, Pageable pageable);

    List<LocationRole> getListByNameAndLocation(List<String> name, List<String> locationId);

    List<LocationRole> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

    List<LocationRole> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);

}