package ca.waaw.repository;

import ca.waaw.domain.SubscriptionList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionListRepository extends JpaRepository<SubscriptionList, String> {

    Optional<SubscriptionList> findOneByEmail(String email);

}