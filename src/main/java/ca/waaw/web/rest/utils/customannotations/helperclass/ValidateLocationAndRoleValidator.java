package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.enumration.Authority;
import ca.waaw.web.rest.utils.customannotations.ValidateLocationAndRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Objects;

public class ValidateLocationAndRoleValidator implements ConstraintValidator<ValidateLocationAndRole, Object> {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    @Override
    public void initialize(ValidateLocationAndRole constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        String role = String.valueOf(PARSER.parseExpression("role").getValue(value));
        String location = String.valueOf(PARSER.parseExpression("locationId").getValue(value));
        String locationRole = String.valueOf(PARSER.parseExpression("locationRoleId").getValue(value));
        if(Arrays.stream(Authority.values()).map(Objects::toString).noneMatch(auth -> auth.equalsIgnoreCase(role)))
            return true;
        if (Authority.valueOf(role).equals(Authority.MANAGER) && StringUtils.isEmpty(location)) {
            return false;
        } else return (!Authority.valueOf(role).equals(Authority.CONTRACTOR) && !Authority.valueOf(role).equals(Authority.EMPLOYEE)) ||
                (!StringUtils.isEmpty(location) && !StringUtils.isEmpty(locationRole));
    }

}
