package com.reviewtap.device;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Genera el QR de un dispositivo en PNG (ZXing) y SVG (vectorial, construido a partir de la matriz). */
@Service
public class QrCodeService {

    private static final Map<EncodeHintType, Object> HINTS = Map.of(
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN, 2,
            EncodeHintType.CHARACTER_SET, "UTF-8");

    public byte[] png(String content, int sizePx) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, HINTS);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }

    public String svg(String content) {
        try {
            // Tamaño 0 → matriz mínima (un módulo = una celda); el SVG escala sin pérdida.
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, HINTS);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            StringBuilder sb = new StringBuilder(4096);
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(w).append(' ').append(h)
                    .append("\" shape-rendering=\"crispEdges\">")
                    .append("<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>")
                    .append("<path fill=\"#000\" d=\"");
            for (int y = 0; y < h; y++) {
                int x = 0;
                while (x < w) {
                    if (matrix.get(x, y)) {
                        int start = x;
                        while (x < w && matrix.get(x, y)) {
                            x++;
                        }
                        sb.append('M').append(start).append(' ').append(y).append('h').append(x - start)
                                .append("v1h-").append(x - start).append('z');
                    } else {
                        x++;
                    }
                }
            }
            sb.append("\"/></svg>");
            return sb.toString();
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }
}
