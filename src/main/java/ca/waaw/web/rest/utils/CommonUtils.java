package ca.waaw.web.rest.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

public class CommonUtils {

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
