package ca.waaw.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedRequestsDto {

    private String id;

    private String waawId;

    private String location;

    private String type;

    private String subType;

    private String raisedBy;

    private String assignedTo;

    private String status;

    private String createdDate;

    private List<RequestHistoryDto> history;

}
