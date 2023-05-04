package ca.waaw.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTimesheetDto {

    private boolean upcomingShift;

    private int shiftsAfterSeconds;

    private int totalTimeWorkedToday;

    private String startDate;

    private String startTime;

    private Instant startTimestamp;

    private String endDate;

    private String endTime;

    private Instant endTimestamp;

}
