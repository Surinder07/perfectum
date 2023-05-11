package ca.waaw.dto.timesheet;

import ca.waaw.dto.DateTimeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetDetailsDto {

    private String id;

    private String shiftId;

    private DateTimeDto start;

    private DateTimeDto end;

    private String duration;

    private String type;

    private String comment;

}