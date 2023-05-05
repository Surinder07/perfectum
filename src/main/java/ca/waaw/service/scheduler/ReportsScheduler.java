package ca.waaw.service.scheduler;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class ReportsScheduler {

    // check preference and generate report (monthly, bimonthly, and weekly)
    @Scheduled(cron = "0 0 1 * * *")
    public void runMonthlyReportGeneration() {
        // check for report generation frequency and generate if
        // 1. type is monthly, and it's end of month
        // 2. type is bimonthly, and it's 15th of any month
        // 3. type is weekly, and its start of week for the organization
    }

}
