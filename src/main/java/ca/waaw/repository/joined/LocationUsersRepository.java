package ca.waaw.repository.joined;

import ca.waaw.domain.joined.LocationUsers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationUsersRepository extends JpaRepository<LocationUsers, String> {
    Page<LocationUsers> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag, Pageable pageable);

    List<LocationUsers> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);
}