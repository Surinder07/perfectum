package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.dto.shifts.NewShiftBatchDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.timeoff.NewTimeOffDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.dto.userdtos.OrganizationPreferences;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.Currency;
import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.PayrollGenerationType;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
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
     * 1. {@link DependentDtoFieldsValidatorType#ROLE_TO_LOCATION_AND_LOCATION_ROLE}
     * Used in Class {@link InviteUserDto}
     * If role is {@link Authority#MANAGER} {@link InviteUserDto#getLocationId()} should not be null
     * If role is {@link Authority#EMPLOYEE} or {@link Authority#CONTRACTOR},
     * {@link InviteUserDto#getLocationId()} or {@link InviteUserDto#getLocationRoleId()}should not be null
     * <p>
     * 2. {@link DependentDtoFieldsValidatorType#SHIFT_USER_ID_TO_LOCATION_ROLE_ID}
     * Used in {@link NewShiftDto}
     * If both userId and locationRoleId are null or empty, it throws an error.
     * <p>
     * 3. {@link DependentDtoFieldsValidatorType#SHIFT_BATCH_LOCATION_ID_TO_USER_ROLE}
     * Used in {@link NewShiftBatchDto}
     * If logged-in user is admin, locationId, locationRoleId or userIds is required
     * <p>
     * 4. {@link DependentDtoFieldsValidatorType#TIME_OFF_USER_ID_TO_USER_ROLE}
     * Used in {@link NewTimeOffDto}
     * If logged-in user is admin or manager, userId cannot be null
     * <p>
     * 5. {@link DependentDtoFieldsValidatorType#ORGANIZATION_PREFERENCES_PAYROLL}
     * Used in {@link OrganizationPreferences}
     * If frequency is set to weekly, day should be passed in dayDate, or else a date (1-31) should be passed.
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
            case SHIFT_BATCH_LOCATION_ID_TO_USER_ROLE:
                try {
                    if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN)) {
                        List<String> userIds = null;
                        String locationId = null;
                        locationRoleId = null;
                        if (PARSER.parseExpression("locationId").getValue(value) != null &&
                                !String.valueOf(PARSER.parseExpression("locationId").getValue(value)).equals(""))
                            locationId = String.valueOf(PARSER.parseExpression("locationId").getValue(value));
                        if (PARSER.parseExpression("locationRoleId").getValue(value) != null &&
                                !String.valueOf(PARSER.parseExpression("locationRoleId").getValue(value)).equals(""))
                            locationRoleId = String.valueOf(PARSER.parseExpression("locationRoleId").getValue(value));
                        if (PARSER.parseExpression("userIds").getValue(value) != null)
                            userIds = ((List<?>) Objects.requireNonNull(PARSER.parseExpression("userIds")
                                    .getValue(value))).stream().map(Objects::toString).collect(Collectors.toList());
                        return StringUtils.isNotEmpty(locationId) || StringUtils.isNotEmpty(locationRoleId) ||
                                (userIds != null && userIds.size() > 0);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            case TIME_OFF_USER_ID_TO_USER_ROLE:
                if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN, Authority.MANAGER)) {
                    return PARSER.parseExpression("userId").getValue(value) != null &&
                            !String.valueOf(PARSER.parseExpression("userId").getValue(value)).equals("");
                }
            case ORGANIZATION_PREFERENCES_PAYROLL:
                String payrollGenerationFrequency = null;
                String dayDateForPayroll = null;
                if (PARSER.parseExpression("payrollGenerationFrequency").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("payrollGenerationFrequency").getValue(value)).equals("")) {
                    payrollGenerationFrequency = String.valueOf(PARSER.parseExpression("payrollGenerationFrequency").getValue(value));
                }
                if (PARSER.parseExpression("dayDateForPayroll").getValue(value) != null &&
                        !String.valueOf(PARSER.parseExpression("dayDateForPayroll").getValue(value)).equals("")) {
                    dayDateForPayroll = String.valueOf(PARSER.parseExpression("dayDateForPayroll").getValue(value));
                }
                boolean error = (StringUtils.isNumeric(dayDateForPayroll) && (Integer.parseInt(dayDateForPayroll) < 0 &&
                        Integer.parseInt(dayDateForPayroll) > 31)) || !EnumUtils.isValidEnum(DaysOfWeek.class, dayDateForPayroll);
                if (!EnumUtils.isValidEnum(PayrollGenerationType.class, payrollGenerationFrequency)) error = true;
                return !error;
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
        }
        return true;
    }

}
