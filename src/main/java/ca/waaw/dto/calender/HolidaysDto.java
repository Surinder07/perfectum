package ca.waaw.dto.calender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidaysDto {

    private String name;

    private String date;

    private String displayDate;

    private String type;

}