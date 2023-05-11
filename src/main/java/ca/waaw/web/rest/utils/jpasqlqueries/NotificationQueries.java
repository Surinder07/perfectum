package ca.waaw.web.rest.utils.jpasqlqueries;

import ca.waaw.repository.NotificationRepository;

public final class NotificationQueries {

    /**
     * used in {@link NotificationRepository}
     */
    public static final String filterNotifications = "SELECT n FROM Notification n WHERE n.userId = ?1 AND (?2 IS NULL OR n.type = ?2) AND " +
            "((?3 IS NULL OR ?4 IS NULL) OR n.createdTime BETWEEN ?3 AND ?4) AND (?5 IS NULL OR n.isRead = ?5) " +
            "AND n.deleteFlag = FALSE ORDER BY createdTime DESC";

}
