package ca.waaw.service.scheduler;

import ca.waaw.domain.Timesheet;
import ca.waaw.domain.joined.DetailedTimesheet;
import ca.waaw.repository.joined.DetailedTimesheetRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
@AllArgsConstructor
public class TimesheetScheduler {

    private final Logger log = LogManager.getLogger(TimesheetScheduler.class);

    private final DetailedTimesheetRepository timesheetRepository;

    /**
     * check for clocked in person who might have forgotten to clock out
     */
    @Scheduled(cron = "0 0/30 * * * *")
    public void checkAndCloseActiveTimers() {
        log.info("Running scheduler to check for active timers");
        List<DetailedTimesheet> updatedTimeSheets = timesheetRepository.getAllActiveTimers()
                .stream()
                .peek(sheet -> {
                    long maxAllowedShift = sheet.getLocationRole().getTotalHoursPerDayMax() == 0 ? 8 : sheet.getLocationRole().getTotalHoursPerDayMax();
                    if ((Instant.now()).isAfter(sheet.getStart().plus(maxAllowedShift, ChronoUnit.HOURS)
                            .plus(10, ChronoUnit.MINUTES))) {
                        sheet.setEnd(Instant.now());
                    }
                }).collect(Collectors.toList());
        timesheetRepository.saveAll(updatedTimeSheets);
        updatedTimeSheets.forEach(sheet -> log.info("Stopping timer for timesheet: {}", sheet));
    }

}
