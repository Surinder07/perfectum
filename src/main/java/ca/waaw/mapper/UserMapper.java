package ca.waaw.mapper;

import ca.waaw.domain.User;
import ca.waaw.dto.RegisterOrganizationDto;
import ca.waaw.dto.RegisterUserDto;
import ca.waaw.dto.UserDetailsDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.web.rest.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

public class UserMapper {

    public static UserDetailsDto entityToDto(User source) {
        UserDetailsDto target = new UserDetailsDto();
        BeanUtils.copyProperties(source, target);
        target.setMobile(source.getCountryCode() + " " + target.getMobile());
        return target;
    }

    public static void updateInvitedUser(RegisterUserDto source, User target) {
        target.setLastModifiedBy(target.getId());
        target.setUsername(source.getUsername());
        if(StringUtils.isNotEmpty(source.getFirstName())) target.setFirstName(source.getFirstName());
        if(StringUtils.isNotEmpty(source.getLastName())) target.setLastName(source.getLastName());
        target.setMobile(source.getMobile());
        target.setCountryCode(source.getCountryCode());
        target.setLangKey(source.getLangKey());
        target.setStatus(EntityStatus.ACTIVE);
    }

    public static User registerDtoToUserEntity(RegisterOrganizationDto source) {
        User target = new User();
        BeanUtils.copyProperties(source, target);
        target.setId(UUID.randomUUID().toString());
        target.setCreatedBy(target.getId());
        target.setLastModifiedBy(target.getId());
        target.setStatus(EntityStatus.PENDING);
        target.setAuthority(Authority.ADMIN);
        target.setActivationKey(CommonUtils.Random.generateRandomKey());
        return target;
    }

}
