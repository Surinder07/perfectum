package ca.waaw.service.scheduler;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppInvoiceConfig;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.payments.PaymentHistory;
import ca.waaw.domain.user.User;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.enumration.*;
import ca.waaw.enumration.payment.PaymentStatus;
import ca.waaw.enumration.payment.TransactionType;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.payments.PaymentHistoryRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.service.AppNotificationService;
import ca.waaw.web.rest.service.PaymentsService;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.MessageConstants;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class PaymentsScheduler {

    private final PaymentHistoryRepository paymentHistoryRepository;

    private final PaymentsService paymentService;

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final ShiftsRepository shiftsRepository;

    private final AppNotificationService appNotificationService;

    private final AppCustomIdConfig appCustomIdConfig;

    private final AppInvoiceConfig appInvoiceConfig;

    private final Logger log = LogManager.getLogger(PaymentsScheduler.class);

    @Scheduled(cron = "0 15 0 * * ?")
    public void checkForTrialPeriodsAndSendNotifications() {
        log.info("Looking for organizations with trial period about to end at {}", Instant.now());
        userOrganizationRepository.getAllTrialEndingWithAdmin()
                .stream()
                .filter(user -> user.getOrganization().getTrialEndDate().minus(3, ChronoUnit.DAYS)
                        .isBefore(Instant.now()))
                .forEach(user -> {
                    NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                            .receiverUsername(user.getUsername())
                            .type(NotificationType.PAYMENT)
                            .language(user.getLangKey())
                            .receiverMail(user.getEmail())
                            .receiverUuid(user.getId())
                            .build();
                    DateTimeDto trialEnd = DateAndTimeUtils.getDateTimeObject(user.getOrganization().getTrialEndDate(),
                            user.getOrganization().getTimezone());
                    appNotificationService.sendApplicationNotification(MessageConstants.trialEnd, notificationInfo,
                            true, trialEnd.getDate(), trialEnd.getTime());
                });
    }

    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    public void checkForPendingPaymentAccounts() {
        log.error("Looking for accounts to deactivate for unpaid invoices {}", Instant.now());
        List<Organization> organizationsToUpdate = new ArrayList<>();
        List<User> usersToUpdate = new ArrayList<>();
        organizationRepository.getAllActiveOrganization()
                .forEach(organization -> paymentHistoryRepository.findOneByOrganizationIdAndPaymentStatus(organization.getId(), PaymentStatus.UNPAID)
                        .filter(payment -> payment.getDueDate().isBefore(Instant.now()))
                        .ifPresent(payment -> {
                            organization.setPaymentPending(true);
                            organizationsToUpdate.add(organization);
                            log.info("Disabling organization for unpaid invoice: {}", organization);
                            userRepository.findAllByOrganizationIdAndDeleteFlag(organization.getId(), false)
                                    .forEach(user -> {
                                        paymentService.notifyUserAboutAccountSuspension(user, payment, organization.getTimezone());
                                        user.setAccountStatus(AccountStatus.PAYMENT_PENDING);
                                        usersToUpdate.add(user);
                                    });
                        })
                );
        organizationRepository.saveAll(organizationsToUpdate);
        userRepository.saveAll(usersToUpdate);
    }

    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
    public void checkForPendingInvoices() {
        // TODO
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void generatePayments() {
        List<PaymentHistory> newPayments = new ArrayList<>();
        log.info("Looking for invoices to generate at {}", Instant.now());
        AtomicReference<String> currentOrgCustomId = new AtomicReference<>(paymentHistoryRepository.getLastUsedInvoiceId()
                .orElse(appCustomIdConfig.getInvoicePrefix() + "0000000000"));
        List<Map<String, Object>> notificationsInfo = new ArrayList<>();
        log.info("Looking for organizations with payment date between {} and {}", Instant.now().minus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS),
                Instant.now().plus(1, ChronoUnit.SECONDS));
        try {
            userOrganizationRepository.getAllActiveOrganizationWithAdmin()
                    .stream()
                    .filter(user -> user.getOrganization().getNextPaymentOn() != null &&
                            user.getOrganization().getNextPaymentOn().isBefore(Instant.now().plus(1, ChronoUnit.SECONDS)) &&
                            user.getOrganization().getNextPaymentOn().isAfter(Instant.now().minus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS)))
                    .forEach(user -> {
                        // TODO change 3 days difference to month
                        Map<String, Object> paymentInfo = new HashMap<>();
                        NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                                .receiverUuid(user.getId())
                                .receiverMail(user.getEmail())
                                .receiverUsername(user.getUsername())
                                .receiverName(user.getFullName())
                                .language(user.getLangKey())
                                .type(NotificationType.PAYMENT)
                                .build();
                        paymentInfo.put("notification", notificationInfo);
                        Instant[] dateRange = DateAndTimeUtils.getStartAndEndTimeForInstant(Instant.now().minus(3, ChronoUnit.DAYS), 3, user.getOrganization().getTimezone());
                        paymentInfo.put("start", DateAndTimeUtils.getDateTimeObject(dateRange[0], user.getOrganization().getTimezone()).getDate());
                        paymentInfo.put("end", DateAndTimeUtils.getDateTimeObject(dateRange[1], user.getOrganization().getTimezone()).getDate());
                        long employees = shiftsRepository.getActiveEmployeesBetweenDates(user.getOrganizationId(), dateRange[0], dateRange[1]);
                        log.info("Checking payment for organization {}({}) between dates {} and {}, found {} active employees",
                                user.getOrganization().getName(), user.getOrganization().getId(), dateRange[0], dateRange[1], employees);
                        if (employees > 0) {
                            int unitPrice = getPrice(employees);
                            PaymentHistory payment = new PaymentHistory();
                            payment.setInvoiceDate(Instant.now());
                            // todo
//                        payment.setDueDate(dateRange[1].plus(appInvoiceConfig.getAllowDaysBeforeDueDate(), ChronoUnit.DAYS));
                            payment.setDueDate(dateRange[1].plus(12, ChronoUnit.HOURS));
                            paymentInfo.put("dueDate", DateAndTimeUtils.getDateTimeObject(payment.getDueDate(), user.getOrganization().getTimezone()).getDate());
                            payment.setInvoiceStart(dateRange[0]);
                            payment.setInvoiceEnd(dateRange[1]);
                            payment.setOrganizationId(user.getOrganizationId());
                            payment.setQuantity((int) employees);
                            payment.setUnitPrice(unitPrice);
                            payment.setTotalAmount(unitPrice * employees);
                            payment.setCurrency(Currency.CAD);
                            payment.setTransactionType(TransactionType.MONTHLY_FEE);
                            payment.setInvoiceId(CommonUtils.getNextCustomId(currentOrgCustomId.get(), appCustomIdConfig.getLength()));
                            newPayments.add(payment);
                            currentOrgCustomId.set(payment.getInvoiceId());
                        }
                        notificationsInfo.add(paymentInfo);
                    });
            paymentHistoryRepository.saveAll(newPayments);
        } catch (Exception e) {
            log.error("Exception while generating paymentss", e);
        }
        notificationsInfo.forEach(notification -> {
            if (notification.containsKey("dueDate")) {
                appNotificationService.sendApplicationNotification(MessageConstants.newInvoice,
                        (NotificationInfoDto) notification.get("notification"), true, notification.get("start").toString(),
                        notification.get("end").toString(), notification.get("dueDate").toString());
            } else {
                appNotificationService.sendApplicationNotification(MessageConstants.noInvoice,
                        (NotificationInfoDto) notification.get("notification"), false, notification.get("start").toString(),
                        notification.get("end").toString());
            }
        });
    }

    private int getPrice(long employees) {
        if (employees < 21) return 20;
        else if (employees < 51) return 18;
        else return 15;
    }

}
