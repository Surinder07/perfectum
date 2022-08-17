package ca.waaw.repository;

import ca.waaw.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {
    Optional<Location> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);
    List<Location> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);
}
