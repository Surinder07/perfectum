package ca.waaw.web.rest.service;

import ca.waaw.domain.Notification;
import ca.waaw.domain.user.User;
import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.mapper.NotificationMapper;
import ca.waaw.repository.NotificationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NotificationService {

    private final Logger log = LogManager.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final UserRepository userRepository;

    public PaginationDto getAllNotifications(int pageNo, int pageSize, String startDate, String endDate, String type,
                                             Boolean isRead) {
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdTime").descending());
        AtomicReference<String> timezone = new AtomicReference<>(null);
        Page<Notification> notificationPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    timezone.set(user.getAuthority().equals(Authority.ADMIN) ? user.getOrganization().getTimezone() :
                            user.getLocation().getTimezone());
                    Instant[] startEnd = StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate) ?
                            DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone.get()) : new Instant[]{null, null};
                    return notificationRepository.searchAndFilterNotification(user.getId(), type, startEnd[0],
                            startEnd[1], isRead, getSortedByCreatedDate);
                })
                .orElse(Page.empty());
        return CommonUtils.getPaginationResponse(notificationPage, NotificationMapper::entityToDto, timezone.get());
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
