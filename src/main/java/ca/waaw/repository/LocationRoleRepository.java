package ca.waaw.repository;

import ca.waaw.domain.LocationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRoleRepository extends JpaRepository<LocationRole, String> {

    Optional<LocationRole> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<LocationRole> getByNameAndLocationId(String name, String locationId);

    List<LocationRole> getListByNameAndLocation(List<String> name, List<String> locationId);

    List<LocationRole> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);

}