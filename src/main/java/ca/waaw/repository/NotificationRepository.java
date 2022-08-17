package ca.waaw.repository;

import ca.waaw.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    Optional<Notification> findOneById(String id);
    List<Notification> findAllByUserId(String userId);
    List<Notification> findAllByUserIdAndIsRead(String userId, boolean isRead);
}
