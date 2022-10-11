package ca.waaw.dto.locationandroledtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationAndRoleDto {

    private String locationId;

    private String locationName;

    private String locationTimezone;

    private String locationRoleId;

    private String locationRoleName;

}
