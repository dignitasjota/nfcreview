package com.reviewtap.common;

import java.security.SecureRandom;

public final class Passwords {

    /** Mínimo 8 caracteres con al menos una letra y un número. */
    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String MESSAGE = "La contraseña debe tener al menos 8 caracteres, con letras y números";

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {}

    /** Contraseña inicial aleatoria (14 caracteres) que cumple la política. */
    public static String generate() {
        while (true) {
            StringBuilder sb = new StringBuilder(14);
            for (int i = 0; i < 14; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (candidate.matches(PATTERN)) {
                return candidate;
            }
        }
    }
}
