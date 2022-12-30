package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class RegexMatcherValidator implements ConstraintValidator<ValidateRegex, CharSequence> {

    private final AppRegexConfig appRegexConfig;

    private RegexValidatorType type;

    public RegexMatcherValidator(AppRegexConfig appRegexConfig) {
        this.appRegexConfig = appRegexConfig;
    }

    @Override
    public void initialize(ValidateRegex annotation) {
        type = annotation.type();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        switch (type) {
            case EMAIL:
                return Pattern.matches(appRegexConfig.getEmail(), value);
            case USERNAME:
                return Pattern.matches(appRegexConfig.getUsername(), value);
            case PASSWORD:
                return Pattern.matches(appRegexConfig.getPassword(), value);
            case EMAIL_USERNAME:
                return Pattern.matches(appRegexConfig.getUsername(), value) ||
                        Pattern.matches(appRegexConfig.getEmail(), value);
            case DATE:
                if (!Pattern.matches(appRegexConfig.getDate(), value)) return false;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    sdf.parse(value.toString());
                    return true;
                } catch (ParseException e) {
                    return false;
                }
            case TIME:
                if (value.length() > 5) return false;
                try {
                    String[] splitValue = value.toString().split(":");
                    if (splitValue[0].length() != 2 || splitValue[1].length() != 2) return false;
                    int hour = Integer.parseInt(splitValue[0]);
                    int minute = Integer.parseInt(splitValue[1]);
                    if (hour > 24 || minute > 59) return false;
                } catch (Exception e) {
                    return false;
                }
                return true;
            case REPORT_FORMAT:
                return String.valueOf(value).equalsIgnoreCase("csv") ||
                        String.valueOf(value).equalsIgnoreCase("xls");
            default:
                return true;
        }
    }

}
