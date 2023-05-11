package ca.waaw.repository.locationandroles;

import ca.waaw.domain.locationandroles.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {

    Optional<Location> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<Location> getByNameAndOrganizationId(String name, String organizationId);

    List<Location> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlg);

    @Query(value = "SELECT waaw_id from location WHERE waaw_id IS NOT NULL ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    Optional<String>getLastUsedWaawId();

    List<Location> getListByNameAndOrganization(List<String> name, String organizationId);

    List<Location> findAllByIdIn(List<String> ids);

}