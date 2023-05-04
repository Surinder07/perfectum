package ca.waaw.service.scheduler;

import ca.waaw.domain.Organization;
import ca.waaw.domain.Shifts;
import ca.waaw.domain.Timesheet;
import ca.waaw.domain.User;
import ca.waaw.dto.NotificationInfoDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.ShiftsRepository;
import ca.waaw.repository.TimesheetRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.service.AppNotificationService;
import ca.waaw.service.WebSocketService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class ShiftsScheduler {

    private final Logger log = LogManager.getLogger(ShiftsScheduler.class);

    private final TimesheetRepository timesheetRepository;

    private final ShiftsRepository shiftsRepository;

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final AppNotificationService appNotificationService;

    private final WebSocketService webSocketService;

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void checkToActivateTimers() {
        log.info("Running scheduler to check for upcoming shift to allow timer");
        List<Organization> organizations = organizationRepository.findAll()
                .stream().filter(organization -> !organization.isDeleteFlag())
                .collect(Collectors.toList());
        int maxTimeGrace = organizations.stream().mapToInt(Organization::getClockInAllowedMinutesBeforeShift)
                .max().orElse(5);
        List<Map<String, Object>> infoToSend = new ArrayList<>();
        Instant start = Instant.now().plus(maxTimeGrace, ChronoUnit.MINUTES).plus(1L, ChronoUnit.SECONDS);
        List<Shifts> allShifts = shiftsRepository.getAllUpcomingOrOngoingShifts(start, Instant.now());
        List<User> users = userRepository.findAllByIdInAndDeleteFlag(allShifts.stream()
                .map(Shifts::getUserId).collect(Collectors.toList()), false);
        organizations.forEach(organization -> {
            int maxTime = organization.getClockInAllowedMinutesBeforeShift();
            allShifts.stream().filter(shift -> shift.getOrganizationId().equals(organization.getId()))
                    .filter(shift -> shift.getStart().isBefore(Instant.now().plus(maxTime, ChronoUnit.MINUTES)
                            .plus(1L, ChronoUnit.SECONDS)))
                    .filter(shift -> shift.getShiftStatus().equals(ShiftStatus.RELEASED))
                    .forEach(shift -> {
                        Map<String, Object> info = new HashMap<>();
                        int timeRemaining = (int) Duration.between(Instant.now(), shift.getStart()).toSeconds();
                        timeRemaining = timeRemaining - (maxTime * 60);
                        if (timeRemaining < 0) timeRemaining = 0;
                        info.put("timerActive", true);
                        info.put("timeRemaining", timeRemaining); // Seconds
                        info.put("user", users.stream().filter(user -> user.getId().equals(shift.getUserId()))
                                .findFirst().map(User::getUsername).orElse(null));
                        infoToSend.add(info);
                    });
        });
        infoToSend.forEach(info -> webSocketService.notifyUserToAllowClockIn(info.get("user").toString(),
                Integer.parseInt(String.valueOf(info.get("timeRemaining")))));
    }

    /**
     * Check if an employee has clocked in for their shift or not
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void checkForMissedShiftsAndSendNotification() {
        Instant end = Instant.now();
        Instant start = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<Timesheet> sheetsToCheck = timesheetRepository.findAllByStartBetweenAndDeleteFlag(start, end, false);
        List<Shifts> shiftsToNotifyFor = shiftsRepository.findAllByStartBetweenAndDeleteFlag(start, end, false)
                .stream().filter(shift -> shift.getShiftStatus().equals(ShiftStatus.RELEASED))
                .filter(shift -> !sheetsToCheck.stream().map(Timesheet::getUserId).collect(Collectors.toList())
                        .contains(shift.getUserId()))
                .collect(Collectors.toList());
        Set<String> organizationIds = shiftsToNotifyFor.stream()
                .map(Shifts::getOrganizationId).collect(Collectors.toSet());
        Set<String> locationIds = shiftsToNotifyFor.stream()
                .map(Shifts::getLocationId).collect(Collectors.toSet());
        List<String> userIds = shiftsToNotifyFor.stream()
                .map(Shifts::getUserId).collect(Collectors.toList());
        List<User> gAdmin = userRepository.findAllByOrganizationIdInAndAuthorityAndDeleteFlag(organizationIds,
                Authority.ADMIN, false);
        List<User> lAdmins = userRepository.findAllByLocationIdInAndAuthorityAndDeleteFlag(locationIds,
                Authority.MANAGER, false);
        List<User> users = userRepository.findAllByIdInAndDeleteFlag(userIds, false);
        notifyAdminsForShifts(shiftsToNotifyFor, gAdmin, lAdmins, users);
    }

    private void notifyAdminsForShifts(List<Shifts> shiftToNotifyFor, List<User> gAdmin, List<User> lAdmins,
                                       List<User> users) {
        try {
            shiftToNotifyFor.forEach(shift -> {
                log.info("Sending notification for missed shift: {}", shift);
                User user = users.stream().filter(user1 -> user1.getId().equals(shift.getUserId())).findFirst().orElse(null);
                assert user != null;
                List<User> admins = gAdmin.stream().filter(admin -> admin.getOrganizationId().equals(shift.getOrganizationId()))
                        .collect(Collectors.toList());
                if (!user.getAuthority().equals(Authority.MANAGER)) {
                    admins.addAll(lAdmins.stream().filter(admin -> admin.getOrganizationId().equals(shift.getOrganizationId()))
                            .collect(Collectors.toList()));
                }
                admins.forEach(admin -> {
                    NotificationInfoDto notificationInfo = NotificationInfoDto
                            .builder()
                            .receiverUuid(admin.getId())
                            .receiverName(admin.getFullName())
                            .receiverUsername(admin.getUsername())
                            .receiverMail(admin.getEmail())
                            .receiverMobile(admin.getMobile() == null ? null : admin.getCountryCode() + admin.getMobile())
                            .language(admin.getLangKey() == null ? null : admin.getLangKey())
                            .type(NotificationType.REQUEST)
                            .build();
                    appNotificationService.sendNotification("notification.shift.missed", notificationInfo, user.getFullName());
                });
                log.info("Sending notification for missed shift successful");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
