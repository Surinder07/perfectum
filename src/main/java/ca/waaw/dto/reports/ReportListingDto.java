package ca.waaw.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportListingDto {

    private String id;

    private String waawId;

    private String from;

    private String to;

    private String createdOn;

    private String locationName;

}