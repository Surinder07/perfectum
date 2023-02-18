package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.shifts.NewShiftDto;
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
     * 2. {@link DependentDtoFieldsValidatorType#ORGANIZATION_PREFERENCES_PAYROLL}
     * Used in {@link OrganizationPreferences}
     * If frequency is set to weekly, day should be passed in dayDate, or else a date (1-31) should be passed.
     * </p>
     * <p>
     * 3. {@link DependentDtoFieldsValidatorType#EMPLOYEE_PREFERENCES_WAGES}
     * Used in {@link EmployeePreferencesDto}
     * If wages are sent in the preferences both amount and currency should be there
     * </p>
     * <p>
     * 4. {@link DependentDtoFieldsValidatorType#LOCATION_ROLE_TO_USER_ROLE}
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
                if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN) && shiftType.equalsIgnoreCase("single")) {
                    List<String> userIds = null;
                    String locationId = null;
                    List<String> locationRoleIds = null;
                    if (PARSER.parseExpression("locationId").getValue(value) != null &&
                            !String.valueOf(PARSER.parseExpression("locationId").getValue(value)).equals(""))
                        locationId = String.valueOf(PARSER.parseExpression("locationId").getValue(value));
                    if (PARSER.parseExpression("locationRoleIds").getValue(value) != null)
                        locationRoleIds = ((List<?>) Objects.requireNonNull(PARSER.parseExpression("locationRoleIds")
                                .getValue(value))).stream().map(Objects::toString).collect(Collectors.toList());
                    if (PARSER.parseExpression("userIds").getValue(value) != null)
                        userIds = ((List<?>) Objects.requireNonNull(PARSER.parseExpression("userIds")
                                .getValue(value))).stream().map(Objects::toString).collect(Collectors.toList());
                    return (StringUtils.isNotEmpty(locationId) && (locationRoleIds != null && locationRoleIds.size() > 0)) ||
                            (userIds != null && userIds.size() > 0);
                }
                return true;
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
