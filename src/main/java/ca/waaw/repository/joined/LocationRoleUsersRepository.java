package ca.waaw.repository.joined;

import ca.waaw.domain.joined.LocationRoleUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRoleUsersRepository extends JpaRepository<LocationRoleUsers, String> {

    List<LocationRoleUsers> findAllByLocationIdAndAdminRightsAndDeleteFlag(String locationId, boolean adminRights, boolean deleteFlag);

}