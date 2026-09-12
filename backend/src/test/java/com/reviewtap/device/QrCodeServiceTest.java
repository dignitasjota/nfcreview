package com.reviewtap.device;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void pngDecodesBackToTheSameUrl() throws Exception {
        String url = "https://r.midominio.com/d/F8k3Lm2Pq7?src=qr";
        byte[] png = service.png(url, 300);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(300);
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(url);
    }

    @Test
    void svgIsWellFormedVector() {
        String svg = service.svg("https://r.midominio.com/d/F8k3Lm2Pq7?src=qr");
        assertThat(svg).startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\"").endsWith("</svg>")
                .contains("viewBox=\"0 0 ").contains("<path fill=\"#000\"");
    }

    @Test
    void publicCodesAreLongRandomAndUrlSafe() {
        PublicCodeGenerator gen = new PublicCodeGenerator();
        var codes = new java.util.HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            String code = gen.generate();
            assertThat(code).hasSize(10).matches("[A-Za-z0-9]+").doesNotContainPattern("[0O1lI]");
            codes.add(code);
        }
        assertThat(codes).hasSize(1000);
        assertThat(PublicCodeGenerator.isPlausible("../etc")).isFalse();
        assertThat(PublicCodeGenerator.isPlausible("F8k3Lm2Pq7")).isTrue();
    }
}
