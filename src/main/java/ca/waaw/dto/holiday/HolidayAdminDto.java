package ca.waaw.dto.holiday;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayAdminDto {

    private String locationId;

    private String locationName;

    private List<HolidayDto> holidays;

}
