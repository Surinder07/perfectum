package ca.waaw.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestHistoryDto {

    private String id;

    private String title;

    private String description;

    private String date;

    private String status;

}
