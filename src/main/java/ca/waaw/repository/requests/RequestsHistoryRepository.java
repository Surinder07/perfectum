package ca.waaw.repository.requests;

import ca.waaw.domain.requests.RequestsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestsHistoryRepository extends JpaRepository<RequestsHistory, String> {
}