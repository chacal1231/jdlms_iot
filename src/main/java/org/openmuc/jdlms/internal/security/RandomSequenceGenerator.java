package org.openmuc.jdlms.internal.security;

import java.security.SecureRandom;

public class RandomSequenceGenerator {

    public static byte[] generate(int challengeLenth) {
        final SecureRandom random = new SecureRandom();

        byte[] result = new byte[challengeLenth];
        random.nextBytes(result);
        return result;
    }

    private RandomSequenceGenerator() {
    }

}
