package com.reviewtap.interaction;

import java.util.Locale;

public enum InteractionType {
    NFC,
    QR,
    UNKNOWN;

    /** Sólo se aceptan valores conocidos del parámetro {@code src}; cualquier otro cuenta como UNKNOWN. */
    public static InteractionType fromSource(String src) {
        if (src == null) {
            return UNKNOWN;
        }
        return switch (src.trim().toLowerCase(Locale.ROOT)) {
            case "nfc" -> NFC;
            case "qr" -> QR;
            default -> UNKNOWN;
        };
    }
}
