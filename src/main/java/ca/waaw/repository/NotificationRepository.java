package ca.waaw.repository;

import ca.waaw.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findOneByIdAndUserIdAndDeleteFlag(String id, String userId, boolean deleteFlag);

    List<Notification> findAllByUserIdAndIsReadAndDeleteFlag(String userId, boolean isRead, boolean deleteFlag);

    Page<Notification> findAllByUserIdAndDeleteFlag(String userId, boolean deleteFlag, Pageable pageable);

    @Query(value = "SELECT n FROM Notification n WHERE n.userId = ?1 AND (?2 IS NULL OR n.type = ?2) AND " +
            "((?3 IS NULL OR ?4 IS NULL) OR n.createdTime BETWEEN ?3 AND ?4) AND (?5 IS NULL OR n.isRead = ?5) " +
            "AND n.deleteFlag = FALSE ORDER BY createdTime DESC")
    Page<Notification> searchAndFilterNotification(String userId, String type, Instant startDate, Instant endDate,
                                                   Boolean isRead, Pageable pageable);

}