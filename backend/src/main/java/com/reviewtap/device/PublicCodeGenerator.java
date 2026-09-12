package com.reviewtap.device;

import java.security.SecureRandom;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Códigos públicos aleatorios para URLs: 10 caracteres de un alfabeto sin ambigüedades
 * (sin 0/O/1/l/I). 57 símbolos^10 ≈ 3,6·10^17 combinaciones (≈58 bits), no secuenciales.
 */
@Component
public class PublicCodeGenerator {

    public static final int LENGTH = 10;
    public static final Pattern VALID = Pattern.compile("^[A-Za-z0-9]{6,32}$");

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static boolean isPlausible(String code) {
        return code != null && VALID.matcher(code).matches();
    }
}
