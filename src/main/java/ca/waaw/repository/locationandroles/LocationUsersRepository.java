package ca.waaw.repository.locationandroles;

import ca.waaw.domain.locationandroles.LocationUsers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationUsersRepository extends JpaRepository<LocationUsers, String> {

    Page<LocationUsers> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag, Pageable pageable);

    List<LocationUsers> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);

    @Query(value = "SELECT l FROM LocationUsers l WHERE (?1 IS NULL OR l.name LIKE CONCAT('%', ?1, '%')) AND " +
            "(?2 IS NULL OR l.isActive = ?2) AND (?3 IS NULL OR l.timezone = ?3) AND l.organizationId = ?4 AND " +
            "l.deleteFlag = FALSE")
    Page<LocationUsers> searchAndFilterLocation(String searchKey, Boolean active, String timezone, String organizationId, Pageable pageable);

}