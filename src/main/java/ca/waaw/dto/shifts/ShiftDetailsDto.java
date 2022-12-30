package ca.waaw.dto.shifts;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDetailsDto {

    private String id;

    private String employeeId;

    private String employeeName;

    private String employeeEmail;

    private String locationName;

    private String locationRoleName;

    private DateTimeDto start;

    private DateTimeDto end;

    private String notes;

    private ShiftStatus shiftStatus;

    private ShiftType shiftType;

}