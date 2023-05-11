package ca.waaw.web.rest.utils.jpasqlqueries;

import ca.waaw.repository.shifts.ShiftDetailsRepository;
import ca.waaw.repository.shifts.ShiftsRepository;

public final class ShiftQueries {

    /**
     * Batch search and filter query parts
     */
    private static final String selectClause = "SELECT batch_id AS id, waaw_batch_id AS batchId, batch_name AS name, " +
            "batch_start AS start, batch_end AS end, created_date AS createdDate, group_concat(DISTINCT shift_status " +
            "ORDER BY shift_status ASC SEPARATOR ' ') AS status FROM shifts ";
    private static final String selectClauseAssigned = "SELECT batch_id AS id, waaw_batch_id AS batchId, batch_name AS name, " +
            "batch_start AS start, batch_end AS end, group_concat(DISTINCT shift_status ORDER BY shift_status ASC " +
            "SEPARATOR ',') AS status FROM shifts ";
    private static final String whereClause = "WHERE (?1 IS NULL OR (waaw_batch_id LIKE CONCAT('%',?1,'%') OR waaw_shift_id " +
            "LIKE CONCAT('%',?1,'%') OR batch_name LIKE CONCAT('%',?1,'%'))) AND (?2 IS NULL OR organization_id = ?2) " +
            "AND (?3 IS NULL OR location_id = ?3) AND (?4 IS NULL OR location_role_id = ?4) AND ((?5 IS NULL AND " +
            "?6 IS NULL) OR (start BETWEEN ?5 AND ?6 OR end BETWEEN ?5 AND ?6)) AND (?5 IS NULL OR start > ?5) AND " +
            "(?7 IS NULL OR shift_status = ?7) AND del_flg = FALSE ";
    private static final String groupByClause = "GROUP BY batch_id ";
    private static final String limitStatusClause = "HAVING FIND_IN_SET(?7, status) > 0";
    private static final String limitAssignedClause = "HAVING FIND_IN_SET('ASSIGNED', status) > 0 OR FIND_IN_SET('CREATED', status) > 0";

    /**
      * Queries for {@link ShiftsRepository}
      */
    public static final String searchAndFilterBatch = selectClause + whereClause + groupByClause;
    public static final String searchAndFilterBatchStatus = selectClause + whereClause + groupByClause + limitStatusClause;
    public static final String searchAndFilterAssignedBatch = selectClauseAssigned + whereClause + groupByClause + limitAssignedClause;
    public static final String lastUsedShiftId = "SELECT waaw_shift_id from shifts WHERE waaw_shift_id IS NOT NULL ORDER BY waaw_shift_id DESC LIMIT 1";
    public static final String lastUsedBatchId = "SELECT waaw_batch_id from shifts WHERE waaw_batch_id IS NOT NULL AND organization_id = ? " +
            "ORDER BY waaw_batch_id DESC LIMIT 1";
    public static final String singleUserBetweenDates = "SELECT s FROM Shifts s WHERE s.userId = ?1 AND (s.start BETWEEN ?2 AND ?3 OR " +
            "s.end BETWEEN ?2 AND ?3) AND s.deleteFlag = FALSE";
    public static final String allUsersBetweenDates = "SELECT s FROM Shifts s WHERE s.userId = ?1 AND (s.start BETWEEN ?2 AND ?3 OR " +
            "s.end BETWEEN ?2 AND ?3) AND s.deleteFlag = FALSE";
    public static final String organizationOrLocationBetweenDates = "SELECT s FROM Shifts s WHERE (?1 IS NULL OR s.organizationId = ?1) " +
            "AND (?2 IS NULL OR s.locationId = ?2) AND ((?3 IS NULL OR ?4 IS NULL) OR (s.start BETWEEN ?3 AND ?4)) AND s.deleteFlag = FALSE";
    public static final String upcomingOrOngoingShifts = "SELECT s FROM Shifts s WHERE s.start < ?1 AND s.end > ?2 AND s.deleteFlag = FALSE";
    public static final String upcomingOrOngoingShiftsUser = "SELECT s FROM Shifts s WHERE s.userId = ?1 AND s.start < ?2 AND s.end > ?3 AND " +
            "s.shiftStatus = 'RELEASED' AND s.deleteFlag = FALSE";
    public static final String activeEmployeeCountBetweenDates = "SELECT COUNT(DISTINCT s.userId) FROM Shifts s WHERE s.organizationId = ?1 AND " +
            "(s.start BETWEEN ?2 AND ?3) AND s.shiftStatus = 'RELEASED' AND s.deleteFlag = FALSE";

    /**
     * Queries for {@link ShiftDetailsRepository}
     */
    public static final String searchAndFilterShifts = "SELECT s from ShiftDetails s LEFT JOIN FETCH s.user u LEFT JOIN FETCH s.location l LEFT JOIN " +
            "FETCH s.locationRole r WHERE (?1 IS NULL OR (u.firstName LIKE CONCAT('%', ?1, '%') OR u.lastName " +
            "LIKE CONCAT('%', ?1, '%') OR u.email LIKE CONCAT('%', ?1, '%'))) AND (?2 IS NULL OR l.id = ?2) AND " +
            "(?3 IS NULL OR r.id = ?3) AND (?4 IS NULL OR s.shiftStatus = ?4) AND (?5 IS NULL OR u.id = ?5) " +
            "AND (?6 = TRUE OR r.adminRights = FALSE) AND ((?7 IS NULL OR ?8 IS NULL) OR (s.start BETWEEN ?7 AND ?8 " +
            "OR s.end BETWEEN ?7 AND ?8)) AND (?7 IS NULL OR s.start > ?7) AND s.batchId IN ?9 AND s.deleteFlag = FALSE";
    public static final String searchAndFilterShiftByUser = "SELECT s FROM ShiftDetails s WHERE (?1 IS NULL OR (s.batchName LIKE " +
            "CONCAT('%', ?1, '%') OR s.waawShiftId LIKE CONCAT('%', ?1, '%') OR s.waawBatchId LIKE CONCAT('%', ?1, '%'))) " +
            "AND (?2 IS NULL OR s.shiftStatus = ?2) AND s.userId = ?3 AND ((?4 IS NULL OR ?5 IS NULL) OR " +
            "(s.start BETWEEN ?4 AND ?5 OR s.end BETWEEN ?4 AND ?5)) AND s.deleteFlag = FALSE " +
            "ORDER BY s.start DESC";
    public static final String getShiftsForDashboard = "SELECT s FROM ShiftDetails s WHERE (?2 IS NULL OR s.start BETWEEN ?1 AND ?2) " +
            "AND (?2 IS NOT NULL OR s.start > ?1) AND (?3 = TRUE OR s.locationRole.adminRights = FALSE) " +
            "AND s.organizationId = ?4 AND (?5 IS NULL OR s.location.id = ?5) AND (?6 IS NULL OR s.userId = ?6) " +
            "AND s.deleteFlag = FALSE AND s.shiftStatus = 'RELEASED'";

}