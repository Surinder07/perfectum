package ca.waaw.repository;

import ca.waaw.domain.Reports;
import ca.waaw.enumration.report.UserReport;
import ca.waaw.web.rest.utils.jpasqlqueries.ReportQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ReportsRepository extends JpaRepository<Reports, String> {

    @Query(value = ReportQueries.customId, nativeQuery = true)
    Optional<String> getLastUsedCustomId();

    Optional<Reports> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = ReportQueries.filterReports)
    Page<Reports> getAllWithFilters(String organizationId, String locationId, boolean isManager, Instant start,
                                    Instant end, UserReport type, Pageable pageable);

}