package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.ValidatorType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class RegexMatcherValidator implements ConstraintValidator<ValidateRegex, CharSequence> {

    private final AppRegexConfig appRegexConfig;

    private ValidatorType type;

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
            default:
                return true;
        }
    }

}
