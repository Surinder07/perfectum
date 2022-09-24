package ca.waaw.repository;

import ca.waaw.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<Organization> findOneByIdAndDeleteFlagAndSubscriptionPlanIsNull(String id, boolean deleteFlag);

}