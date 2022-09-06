package ca.waaw.repository;

import ca.waaw.domain.joined.LocationRolesUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRolesUserRepository extends JpaRepository<LocationRolesUser, String> {

    Optional<LocationRolesUser> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

}