package ca.waaw.dto.calender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetDto {

    private String startDatetime;

    private String endDatetime;

}