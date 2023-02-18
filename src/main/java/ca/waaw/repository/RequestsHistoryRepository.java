package ca.waaw.repository;

import ca.waaw.domain.RequestsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestsHistoryRepository extends JpaRepository<RequestsHistory, String> {
}