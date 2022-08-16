package ca.waaw.web.rest.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.ResourceBundle;

public class CommonUtils {


    /**
     * Will fetch the property from {@code messages.properties} resource bundle
     *
     * @param property property key
     * @param locale   language needed for property
     * @return Value for the property key
     */
    public static String getPropertyFromMessagesResourceBundle(String property, Locale locale) {
        if (locale == null) locale = Locale.ENGLISH;
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        return bundle.getString(property);
    }
    public final static class Random {
        private static final int DEF_COUNT = 20;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private Random() {
        }

        public static String generateRandomKey() {
            return RandomStringUtils.random(DEF_COUNT, 0, 0, true, true, null, SECURE_RANDOM);
        }

        static {
            SECURE_RANDOM.nextBytes(new byte[64]);
        }
    }

}
