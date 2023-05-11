package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeDto {

    private int hours;

    private int minutes;

    @Override
    public String toString() {
        return StringUtils.leftPad(String.valueOf(hours), 2, "0") +
                ":" + StringUtils.leftPad(String.valueOf(minutes), 2, "0");
    }
}