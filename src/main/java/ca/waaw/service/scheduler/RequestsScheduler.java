package ca.waaw.service.scheduler;

import ca.waaw.domain.Requests;
import ca.waaw.domain.User;
import ca.waaw.dto.NotificationInfoDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.RequestStatus;
import ca.waaw.enumration.RequestType;
import ca.waaw.repository.RequestsRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.service.NotificationInternalService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class RequestsScheduler {

    private final Logger log = LogManager.getLogger(RequestsScheduler.class);

    private final RequestsRepository requestsRepository;

    private final UserRepository userRepository;

    private final NotificationInternalService notificationInternalService;

    @Scheduled(cron = "0 0 0 * * *")
    public void checkForOpenRequestsAndSendNotification() {
        Instant start = Instant.now();
        Instant end = Instant.now().plus(3, ChronoUnit.DAYS);
        List<Requests> requestToNotifyFor = requestsRepository.findAllByStartBetweenAndDeleteFlag(start, end, false)
                .stream().filter(request-> request.getStatus().equals(RequestStatus.OPEN) ||
                        request.getStatus().equals(RequestStatus.NEW))
                .collect(Collectors.toList());
        //todo update logic
        List<Requests> infoUpdateRequests = requestsRepository.findAllByCreatedDateBetweenAndTypeAndDeleteFlag(
                start, end, RequestType.INFORMATION_UPDATE, false)
                .stream().filter(request-> request.getStatus().equals(RequestStatus.OPEN) ||
                        request.getStatus().equals(RequestStatus.NEW))
                .collect(Collectors.toList());
        requestToNotifyFor.addAll(infoUpdateRequests);
        Set<String> organizationIds = requestToNotifyFor.stream()
                .map(Requests::getOrganizationId).collect(Collectors.toSet());
        Set<String> locationIds = requestToNotifyFor.stream()
                .map(Requests::getLocationId).collect(Collectors.toSet());
        List<String> userIds = requestToNotifyFor.stream()
                .map(Requests::getUserId).collect(Collectors.toList());
        List<User> gAdmin = userRepository.findAllByOrganizationIdInAndAuthorityAndDeleteFlag(organizationIds,
                Authority.ADMIN, false);
        List<User> lAdmins = userRepository.findAllByLocationIdInAndAuthorityAndDeleteFlag(locationIds,
                Authority.MANAGER, false);
        List<User> users = userRepository.findAllByIdInAndDeleteFlag(userIds, false);
        notifyAdminsForRequests(requestToNotifyFor, gAdmin, lAdmins, users);
    }

    private void notifyAdminsForRequests(List<Requests> requestToNotifyFor, List<User> gAdmin, List<User> lAdmins,
                                         List<User> users) {
        requestToNotifyFor.forEach(request -> {
            log.info("Sending notification for pending request: {}", request);
            User user = users.stream().filter(user1 -> user1.getId().equals(request.getUserId())).findFirst().orElse(null);
            assert user != null;
            List<User> admins = gAdmin.stream().filter(admin -> admin.getOrganizationId().equals(request.getOrganizationId()))
                    .collect(Collectors.toList());
            if (!user.getAuthority().equals(Authority.MANAGER)) {
                admins.addAll(lAdmins.stream().filter(admin -> admin.getOrganizationId().equals(request.getOrganizationId()))
                        .collect(Collectors.toList()));
            }
            admins.forEach(admin -> {
                NotificationInfoDto notificationInfo = NotificationInfoDto
                        .builder()
                        .receiverUuid(admin.getId())
                        .receiverName(admin.getFullName())
                        .receiverMail(admin.getEmail())
                        .receiverMobile(admin.getMobile() == null ? null : admin.getCountryCode() + admin.getMobile())
                        .language(admin.getLangKey() == null ? null : admin.getLangKey())
                        .type(NotificationType.REQUEST)
                        .build();
                String requestType = request.getType().toString().toLowerCase().replaceAll("_", " ");
                notificationInternalService.sendNotification("notification.request.pending", notificationInfo, requestType, user.getFullName());
            });
            log.info("Sending notification for pending request successful");
        });
    }

}