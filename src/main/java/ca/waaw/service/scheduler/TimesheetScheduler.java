package ca.waaw.service.scheduler;

import ca.waaw.domain.timesheet.DetailedTimesheet;
import ca.waaw.repository.timesheet.DetailedTimesheetRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void checkAndCloseActiveTimers() {
        log.info("Running scheduler to check for active timers");
        List<DetailedTimesheet> updatedTimeSheets = timesheetRepository.getAllActiveTimers()
                .stream()
                .peek(sheet -> {
                    long maxAllowedShift = sheet.getLocationRole().getTotalMinutesPerDayMax() == 0 ? 8 : sheet.getLocationRole().getTotalMinutesPerDayMax();
                    if ((Instant.now()).isAfter(sheet.getStart().plus(maxAllowedShift, ChronoUnit.MINUTES)
                            .plus(10, ChronoUnit.MINUTES))) {
                        sheet.setEnd(Instant.now());
                    }
                }).collect(Collectors.toList());
        timesheetRepository.saveAll(updatedTimeSheets);
        updatedTimeSheets.forEach(sheet -> log.info("Stopping timer for timesheet: {}", sheet));
    }

}
