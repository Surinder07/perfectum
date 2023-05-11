package ca.waaw.service.scheduler;

import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.user.User;
import ca.waaw.dto.appnotifications.MultipleNotificationDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.repository.requests.RequestsRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.service.AppNotificationService;
import ca.waaw.web.rest.utils.MessageConstants;
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

    private final AppNotificationService appNotificationService;

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
        List<MultipleNotificationDto> notifications = new ArrayList<>();
        pendingRequests.forEach(request -> users.stream().filter(user -> user.getId().equals(request.getUserId())).findFirst()
                .ifPresent(requestor -> organizationAdminUserMap.get(request.getOrganizationId())
                        .forEach(admin -> {
                            if (!requestor.getAuthority().equals(Authority.MANAGER) || admin.getAuthority().equals(Authority.ADMIN)) {
                                String requestType = request.getType().toString().toLowerCase().replaceAll("_", " ");
                                MultipleNotificationDto notification = MultipleNotificationDto.builder()
                                        .messageConstant(MessageConstants.pendingRequest)
                                        .messageArguments(new String[]{requestType, requestor.getFullName()})
                                        .notificationInfo(
                                                NotificationInfoDto
                                                        .builder()
                                                        .receiverUuid(admin.getId())
                                                        .receiverUsername(admin.getUsername())
                                                        .receiverName(admin.getFullName())
                                                        .receiverMail(admin.getEmail())
                                                        .receiverMobile(admin.getMobile() == null ? null : admin.getCountryCode() + admin.getMobile())
                                                        .language(admin.getLangKey() == null ? null : admin.getLangKey())
                                                        .type(NotificationType.REQUEST)
                                                        .build()
                                        )
                                        .build();
                                notifications.add(notification);
                            }
                        })));
        appNotificationService.sendMultipleApplicationNotification(notifications);
    }

}