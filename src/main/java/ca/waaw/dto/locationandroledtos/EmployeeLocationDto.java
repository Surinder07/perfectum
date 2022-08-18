package ca.waaw.dto.locationandroledtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EmployeeLocationDto extends NewLocationDto {

    private String id;

    private LocationRoleDto locationRoles;

}