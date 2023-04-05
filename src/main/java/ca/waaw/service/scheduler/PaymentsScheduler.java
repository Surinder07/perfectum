package ca.waaw.service.scheduler;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppInvoiceConfig;
import ca.waaw.domain.Invoices;
import ca.waaw.enumration.Currency;
import ca.waaw.enumration.TransactionType;
import ca.waaw.repository.InvoicesRepository;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.ShiftsRepository;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class PaymentsScheduler {

    private final InvoicesRepository invoicesRepository;

    private final OrganizationRepository organizationRepository;

    private final ShiftsRepository shiftsRepository;

    private final AppCustomIdConfig appCustomIdConfig;

    private final AppInvoiceConfig appInvoiceConfig;

    private final Logger log = LogManager.getLogger(PaymentsScheduler.class);

    @Scheduled(cron = "0 5 0 * * ?")
    public void checkForTrialPeriodsAndSendNotifications() {
        // TODO
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void checkForPendingPaymentAccounts() {
        // TODO
    }

    //    @Scheduled(cron = "0 15 0 */3 * ?")
    @Scheduled(cron = "0 5 0 * * ?")
    public void generateInvoices() {
        List<Invoices> newInvoices = new ArrayList<>();
        log.info("Looking for invoices to generate at {}", Instant.now());
        AtomicReference<String> currentOrgCustomId = new AtomicReference<>(invoicesRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getInvoicePrefix() + "0000000000"));
        organizationRepository.getAllActiveOrganization()
                .stream()
                .filter(organization -> organization.getNextPaymentOn() != null &&
                        organization.getNextPaymentOn().isBefore(Instant.now()) &&
                        organization.getNextPaymentOn().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .forEach(organization -> {
                    Instant[] dateRange = DateAndTimeUtils.getStartAndEndTimeForInstant(Instant.now().minus(3, ChronoUnit.DAYS), 3, organization.getTimezone());
                    long employees = shiftsRepository.getActiveEmployeesBetweenDates(organization.getId(), dateRange[0], dateRange[1]);
                    int unitPrice = getPrice(employees);
                    Invoices invoice = new Invoices();
                    invoice.setInvoiceDate(Instant.now());
                    invoice.setDueDate(dateRange[1].plus(appInvoiceConfig.getAllowDaysBeforeDueDate(), ChronoUnit.DAYS));
                    invoice.setInvoiceStart(dateRange[0]);
                    invoice.setInvoiceEnd(dateRange[1]);
                    invoice.setOrganizationId(organization.getId());
                    invoice.setQuantity((int) employees);
                    invoice.setUnitPrice(unitPrice);
                    invoice.setTotalAmount(unitPrice * employees);
                    invoice.setCurrency(Currency.CAD);
                    invoice.setTransactionType(TransactionType.MONTHLY_FEE);
                    invoice.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId.get(), appCustomIdConfig.getLength()));
                    newInvoices.add(invoice);
                    currentOrgCustomId.set(invoice.getWaawId());
                });
        invoicesRepository.saveAll(newInvoices);
    }

    private int getPrice(long employees) {
        if (employees < 21) return 20;
        else if (employees < 51) return 18;
        else return 15;
    }

}
