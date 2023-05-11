package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.dto.userdtos.EmployeePreferencesDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.Currency;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DependentDtoFieldsValidator implements ConstraintValidator<ValidateDependentDtoField, Object> {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private DependentDtoFieldsValidatorType type;

    @Override
    public void initialize(ValidateDependentDtoField annotation) {
        type = annotation.type();
    }

    /**
     * <p>
     * 1. {@link DependentDtoFieldsValidatorType#NEW_SHIFT_REQUIRED_FIELD}
     * Used in {@link NewShiftDto}
     * Check for different validations required.
     * </p>
     * <p>
     * 2. {@link DependentDtoFieldsValidatorType#EMPLOYEE_PREFERENCES_WAGES}
     * Used in {@link EmployeePreferencesDto}
     * If wages are sent in the preferences both amount and currency should be there
     * </p>
     * <p>
     * 3. {@link DependentDtoFieldsValidatorType#LOCATION_ROLE_TO_USER_ROLE}
     * Used in various DTOs
     * If logged-in user ha role of ADMIN locationId is required to be sent in request
     * </p>
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        switch (type) {
            case NEW_SHIFT_REQUIRED_FIELD:
                String shiftType = null;
                if (PARSER.parseExpression("type").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("type").getValue(value)).equals(""))
                    shiftType = String.valueOf(PARSER.parseExpression("type").getValue(value));
                if (shiftType == null ||
                        !(shiftType.equalsIgnoreCase("SINGLE") || shiftType.equalsIgnoreCase("BATCH"))) {
                    return false;
                }
                List<String> userIds = null;
                if (PARSER.parseExpression("userIds").getValue(value) != null)
                    userIds = ((List<?>) Objects.requireNonNull(PARSER.parseExpression("userIds")
                            .getValue(value))).stream().map(Objects::toString).collect(Collectors.toList());
                if (shiftType.equalsIgnoreCase("single")) {
                    return userIds != null && userIds.size() > 0;
                }
                return true;
            case EMPLOYEE_PREFERENCES_WAGES:
                float wagesPerHour = 0;
                String wagesCurrency = null;
                if (PARSER.parseExpression("wagesPerHour").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("wagesPerHour").getValue(value)).equals("")) {
                    wagesPerHour = Float.parseFloat(String.valueOf(PARSER.parseExpression("wagesPerHour").getValue(value)));
                }
                if (PARSER.parseExpression("wagesCurrency").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("wagesCurrency").getValue(value)).equals("")) {
                    wagesCurrency = String.valueOf(PARSER.parseExpression("wagesCurrency").getValue(value));
                }
                return !(wagesPerHour > 0 && StringUtils.isEmpty(wagesCurrency) && EnumUtils.isValidEnum(Currency.class, wagesCurrency));
            case LOCATION_ROLE_TO_USER_ROLE:
                if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN)) {
                    String locationId = null;
                    if (PARSER.parseExpression("locationId").getValue(value) != null &&
                            !String.valueOf(PARSER.parseExpression("locationId").getValue(value)).equals(""))
                        locationId = String.valueOf(PARSER.parseExpression("locationId").getValue(value));
                    return StringUtils.isNotEmpty(locationId);
                }
                return true;
        }
        return true;
    }

}
