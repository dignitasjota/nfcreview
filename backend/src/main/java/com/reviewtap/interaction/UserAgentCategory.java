package com.reviewtap.interaction;

import java.util.Locale;

/** Categoría gruesa del dispositivo del visitante. Nunca se guarda el User-Agent completo. */
public enum UserAgentCategory {
    MOBILE,
    TABLET,
    DESKTOP,
    BOT,
    UNKNOWN;

    public static UserAgentCategory classify(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider") || ua.contains("preview")
                || ua.contains("curl") || ua.contains("wget") || ua.contains("python-requests")) {
            return BOT;
        }
        if (ua.contains("ipad") || (ua.contains("android") && !ua.contains("mobile")) || ua.contains("tablet")) {
            return TABLET;
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return MOBILE;
        }
        return DESKTOP;
    }
}
