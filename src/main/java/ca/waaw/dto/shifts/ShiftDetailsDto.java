package ca.waaw.dto.shifts;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.shift.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDetailsDto {

    private String id;

    private String name;

    private String waawId;

    private String employeeId;

    private String employeeName;

    private String employeeEmail;

    private String locationName;

    private String locationRoleName;

    private DateTimeDto start;

    private DateTimeDto end;

    private String comments;

    private String failureReason;

    private List<String> conflicts;

    private ShiftStatus shiftStatus;

    private ShiftType shiftType;

}