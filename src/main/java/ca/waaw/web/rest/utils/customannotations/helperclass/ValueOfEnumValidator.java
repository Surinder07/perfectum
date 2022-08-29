package ca.waaw.web.rest.utils.customannotations.helperclass;

import ca.waaw.enumration.Timezones;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, CharSequence> {

    private List<String> acceptedValues;

    @Override
    public void initialize(ValueOfEnum annotation) {
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
                .map(enumVal -> {
                    if (enumVal.getClass().equals(Timezones.class)) {
                        return ((Timezones) enumVal).value;
                    }
                    return enumVal.name();
                })
                .map(String::toLowerCase)
                .filter(name -> !name.equalsIgnoreCase("anonymous"))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return acceptedValues.contains(value.toString().toLowerCase(Locale.ROOT));
    }

}
