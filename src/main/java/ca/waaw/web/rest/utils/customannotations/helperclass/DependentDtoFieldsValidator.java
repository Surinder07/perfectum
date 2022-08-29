package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.enumration.Authority;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Objects;

public class DependentDtoFieldsValidator implements ConstraintValidator<ValidateDependentDtoField, Object> {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private DependentDtoFieldsValidatorType type;

    @Override
    public void initialize(ValidateDependentDtoField annotation) {
        type = annotation.type();
    }

    /**
     * 1. {@link DependentDtoFieldsValidatorType#ROLE_TO_LOCATION_AND_LOCATION_ROLE}
     * Used in Class {@link InviteUserDto}
     * If role is {@link Authority#MANAGER} {@link InviteUserDto#getLocationId()} should not be null
     * If role is {@link Authority#EMPLOYEE} or {@link Authority#CONTRACTOR},
     * {@link InviteUserDto#getLocationId()} or {@link InviteUserDto#getLocationRoleId()}should not be null
     * <p>
     * 2. {@link DependentDtoFieldsValidatorType#SHIFT_USER_ID_TO_LOCATION_ROLE_ID}
     * Used in {@link NewShiftDto}
     * If both userId and locationRoleId are null or empty, it throws an error.
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        switch (type) {
            case ROLE_TO_LOCATION_AND_LOCATION_ROLE:
                if (PARSER.parseExpression("role").getValue(value) == null) return false;
                String role = String.valueOf(PARSER.parseExpression("role").getValue(value));
                if (Arrays.stream(Authority.values()).map(Objects::toString).noneMatch(auth -> auth.equals(role)))
                    return false;
                else if (Authority.valueOf(role).equals(Authority.MANAGER))
                    return PARSER.parseExpression("locationId").getValue(value) != null &&
                            !String.valueOf(PARSER.parseExpression("locationId").getValue(value)).equals("");
                else if (Authority.valueOf(role).equals(Authority.EMPLOYEE))
                    return PARSER.parseExpression("locationId").getValue(value) != null &&
                            PARSER.parseExpression("locationRoleId").getValue(value) != null &&
                            !String.valueOf(PARSER.parseExpression("locationId").getValue(value)).equals("") &&
                            !String.valueOf(PARSER.parseExpression("locationRoleId").getValue(value)).equals("");
                return true;
            case SHIFT_USER_ID_TO_LOCATION_ROLE_ID:
                String userId = null;
                String locationRoleId = null;
                if (PARSER.parseExpression("userId").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("userId").getValue(value)).equals(""))
                    userId = String.valueOf(PARSER.parseExpression("userId").getValue(value));
                if (PARSER.parseExpression("locationRoleId").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("locationRoleId").getValue(value)).equals(""))
                    locationRoleId = String.valueOf(PARSER.parseExpression("locationRoleId").getValue(value));
                return !(userId == null && locationRoleId == null);
            default:
                return true;
        }
    }

}
