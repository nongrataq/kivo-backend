package com.kivo.backend.sharing.infrastructure;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kivo.backend.sharing.application.QrCodeGenerator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ZxingQrCodeGenerator implements QrCodeGenerator {

  @Override
  public byte[] generatePng(String content, int size) {
    try {
      BitMatrix matrix =
          new QRCodeWriter()
              .encode(
                  content,
                  BarcodeFormat.QR_CODE,
                  size,
                  size,
                  Map.of(
                      EncodeHintType.ERROR_CORRECTION,
                      ErrorCorrectionLevel.M,
                      EncodeHintType.MARGIN,
                      2));
      BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      int black = Color.BLACK.getRGB();
      int white = Color.WHITE.getRGB();
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          image.setRGB(x, y, matrix.get(x, y) ? black : white);
        }
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      if (!ImageIO.write(image, "png", output)) {
        throw new IllegalStateException("PNG writer is not available");
      }
      return output.toByteArray();
    } catch (WriterException | IOException exception) {
      throw new IllegalStateException("Failed to generate QR code", exception);
    }
  }
}
