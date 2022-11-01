package ca.waaw.repository;

import ca.waaw.domain.joined.UserReports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportsRepository extends JpaRepository<UserReports, String> {
}