package ca.waaw.dto.locationandroledtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationRoleDto extends BaseLocationRole {

    private String id;

    @Schema(description = "Needed in case of global admin, Not needed while updating location role")
    private String locationId;

}