package ca.waaw.repository;

import ca.waaw.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    Optional<Notification> findOneByIdAndUserIdAndDeleteFlag(String id, String userId, boolean deleteFlag);
    List<Notification> findAllByUserIdAndIsReadAndDeleteFlag(String userId, boolean isRead, boolean deleteFlag);
    Page<Notification> findAllByUserIdAndDeleteFlag(String userId, boolean deleteFlag, Pageable pageable);
}
