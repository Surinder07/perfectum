package ca.waaw.dto.requests;

import ca.waaw.enumration.request.RequestResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestDto {

    private String id;

    private RequestResponse response;

    private String comment;

}