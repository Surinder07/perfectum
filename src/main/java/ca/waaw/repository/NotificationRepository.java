package ca.waaw.repository;

import ca.waaw.domain.Notification;
import ca.waaw.web.rest.utils.jpasqlqueries.NotificationQueries;
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

    @Query(value = NotificationQueries.filterNotifications)
    Page<Notification> searchAndFilterNotification(String userId, String type, Instant startDate, Instant endDate,
                                                   Boolean isRead, Pageable pageable);

}