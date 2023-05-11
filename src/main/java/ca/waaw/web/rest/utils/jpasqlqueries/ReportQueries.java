package ca.waaw.web.rest.utils.jpasqlqueries;

import ca.waaw.repository.ReportsRepository;

public final class ReportQueries {

    /**
     * Queries used in {@link ReportsRepository}
     */
    public static final String customId = "SELECT waaw_id from reports WHERE waaw_id IS NOT NULL ORDER BY waaw_id DESC LIMIT 1";
    public static final String filterReports = "SELECT r FROM Reports r WHERE r.organizationId = ?1 AND (?2 IS NULL OR r.locationId = ?2) " +
            "AND r.showToManger = ?3 AND ((?4 IS NULL OR ?5 IS NULL) OR r.createdDate BETWEEN ?4 AND ?5) " +
            "AND r.type = ?6 AND r.deleteFlag = FALSE ORDER BY r.createdDate DESC";

}
