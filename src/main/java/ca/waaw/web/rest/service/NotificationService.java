package ca.waaw.web.rest.service;

import ca.waaw.domain.Notification;
import ca.waaw.domain.User;
import ca.waaw.dto.PaginationDto;
import ca.waaw.mapper.NotificationMapper;
import ca.waaw.repository.NotificationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NotificationService {

    private final Logger log = LogManager.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    public PaginationDto getAllNotifications(int pageNo, int pageSize) {
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdTime").descending());
        Page<Notification> notificationPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> notificationRepository.findAllByUserIdAndDeleteFlag(user.getId(), false, getSortedByCreatedDate))
                .orElse(Page.empty());
        return CommonUtils.getPaginationResponse(notificationPage, NotificationMapper::entityToDto);
    }

    public void markNotificationAsRead(String id) {
        log.info("Marking notification as read, id: {}", id);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(User::getId)
                .flatMap(userId -> notificationRepository.findOneByIdAndUserIdAndDeleteFlag(id, userId, false))
                .map(notification -> {
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                })
                .map(notification -> CommonUtils.logMessageAndReturnObject(notification, "info", NotificationService.class,
                        "Notification marked as read, id: {}", id))
                .orElseThrow(() -> new EntityNotFoundException("notification"));
    }

    public void markAllNotificationAsRead() {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(User::getId)
                .map(userId -> notificationRepository.findAllByUserIdAndIsReadAndDeleteFlag(userId, false, false)
                        .stream().peek(notification -> notification.setRead(true)).collect(Collectors.toList()))
                .map(notificationRepository::saveAll)
                .ifPresent(notification -> log.info("All notifications marked as read"));
    }

    public void deleteNotification(String id) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(User::getId)
                .flatMap(userId -> notificationRepository.findOneByIdAndUserIdAndDeleteFlag(id, userId, false))
                .map(notification -> {
                    notification.setDeleteFlag(true);
                    return notificationRepository.save(notification);
                })
                .map(notification -> CommonUtils.logMessageAndReturnObject(notification, "info", NotificationService.class,
                        "Notification deleted, id: {}", id))
                .orElseThrow(() -> new EntityNotFoundException("notification"));
    }

}
