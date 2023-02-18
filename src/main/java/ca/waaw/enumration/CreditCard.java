package ca.waaw.enumration;

import java.util.regex.Pattern;

public enum CreditCard {

    UNKNOWN,
    VISA("^4[0-9]{12}(?:[0-9]{3}){0,2}$"),
    MASTERCARD("^(?:5[1-5]|2(?!2([01]|20)|7(2[1-9]|3))[2-7])\\d{14}$"),
    AMERICAN_EXPRESS("^3[47][0-9]{13}$"),
    DINERS_CLUB("^3(?:0[0-5]\\d|095|6\\d{0,2}|[89]\\d{2})\\d{12,15}$"),
    DISCOVER("^6(?:011|[45][0-9]{2})[0-9]{12}$"),
    JCB("^(?:2131|1800|35\\d{3})\\d{11}$"),
    CHINA_UNION_PAY("^62[0-9]{14,17}$");

    private final Pattern pattern;

    CreditCard() {
        this.pattern = null;
    }

    CreditCard(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public static CreditCard detect(String cardNumber) {

        for (CreditCard cardType : CreditCard.values()) {
            if (null == cardType.pattern) continue;
            if (cardType.pattern.matcher(cardNumber).matches()) return cardType;
        }

        return UNKNOWN;
    }

}
