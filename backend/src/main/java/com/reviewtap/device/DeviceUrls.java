package com.reviewtap.device;

import com.reviewtap.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * URLs públicas de un dispositivo. Un único código con {@code ?src=nfc|qr}: la misma placa física
 * es un solo registro y las estadísticas distinguen el canal por el parámetro (validado en
 * {@link com.reviewtap.interaction.InteractionType#fromSource}).
 */
@Component
public class DeviceUrls {

    private final String base;

    public DeviceUrls(AppProperties props) {
        this.base = props.normalizedPublicBaseUrl();
    }

    public String nfc(String publicCode) {
        return base + "/d/" + publicCode + "?src=nfc";
    }

    public String qr(String publicCode) {
        return base + "/d/" + publicCode + "?src=qr";
    }
}
