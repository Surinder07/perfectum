package ca.waaw.dto.locationandroledtos;

import ca.waaw.dto.userdtos.BaseUserDetails;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationRoleWithUsersDto extends BaseLocationRole {

    List<BaseUserDetails> users;

}
