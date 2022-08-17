package ca.waaw.repository;

import ca.waaw.domain.joined.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {
    Optional<UserOrganization> findOneByUsernameAndDeleteFlag(String username, boolean deleteFlag);
    List<UserOrganization> findAllByOrganizationIdAndDeleteFlag(String organizationId, boolean deleteFlag);
    List<UserOrganization> findAllByLocationIdAndDeleteFlag(String locationId, boolean deleteFlag);
}
