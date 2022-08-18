package ca.waaw.dto.locationandroledtos;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AdminLocationDto extends NewLocationDto {

    private String id;

    private List<LocationRoleDto> locationRoles;

}