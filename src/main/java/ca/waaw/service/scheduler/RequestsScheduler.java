package ca.waaw.service.scheduler;

import ca.waaw.domain.Requests;
import ca.waaw.domain.User;
import ca.waaw.dto.NotificationInfoDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.RequestStatus;
import ca.waaw.repository.RequestsRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.service.NotificationInternalService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class RequestsScheduler {

    private final Logger log = LogManager.getLogger(RequestsScheduler.class);

    private final RequestsRepository requestsRepository;

    private final UserRepository userRepository;

    private final NotificationInternalService notificationInternalService;

    @Scheduled(fixedRate = 14400, timeUnit = TimeUnit.SECONDS)
    private void sendNotificationForPendingRequests() {
        List<Requests> pendingRequests = requestsRepository.findAllByDeleteFlagAndStatusIn(false, Arrays.asList(RequestStatus.NEW, RequestStatus.OPEN));
        List<String> uniqueOrganizationIds = pendingRequests.stream().filter(Objects::nonNull).map(Requests::getOrganizationId).distinct().collect(Collectors.toList());
        Map<String, List<User>> organizationAdminUserMap = userRepository.findAllByOrganizationIdInAndAuthorityInAndDeleteFlag(uniqueOrganizationIds, Arrays.asList(Authority.ADMIN, Authority.MANAGER), false)
                .stream().collect(
                        Collectors.groupingBy(User::getOrganizationId)
                );
        List<User> users = Optional.of(pendingRequests.stream()
                        .map(Requests::getUserId)
                        .collect(Collectors.toList()))
                .map(userIds -> userRepository.findAllByIdInAndDeleteFlag(userIds, false))
                .orElse(new ArrayList<>());
        pendingRequests.forEach(req -> log.info("Found an open/new request: {}", req));
        sendNotification(organizationAdminUserMap, pendingRequests, users);
        log.info("Sending notification for pending request successful");
    }

    private void sendNotification(Map<String, List<User>> organizationAdminUserMap, List<Requests> pendingRequests, List<User> users) {
        pendingRequests.forEach(request -> users.stream().filter(user -> user.getId().equals(request.getUserId())).findFirst()
                .ifPresent(requestor -> organizationAdminUserMap.get(request.getOrganizationId())
                        .forEach(admin -> {
                            if (!requestor.getAuthority().equals(Authority.MANAGER) || admin.getAuthority().equals(Authority.ADMIN)) {
                                NotificationInfoDto notificationInfo = NotificationInfoDto
                                        .builder()
                                        .receiverUuid(admin.getId())
                                        .receiverUsername(admin.getUsername())
                                        .receiverName(admin.getFullName())
                                        .receiverMail(admin.getEmail())
                                        .receiverMobile(admin.getMobile() == null ? null : admin.getCountryCode() + admin.getMobile())
                                        .language(admin.getLangKey() == null ? null : admin.getLangKey())
                                        .type(NotificationType.REQUEST)
                                        .build();
                                String requestType = request.getType().toString().toLowerCase().replaceAll("_", " ");
                                notificationInternalService.sendNotification("notification.request.pending", notificationInfo, requestType, requestor.getFullName());
                            }
                        })));
    }

}