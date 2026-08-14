package com.kivo.backend.sharing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.sharing.application.SharedRouteTokenHasher;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SharedRouteTokenAndQrTest {

  @Test
  void generatesUniqueUrlSafeTokensWith256BitsOfEntropy() {
    var generator = new SecureRandomSharedRouteTokenGenerator();
    var tokens = new HashSet<String>();

    for (int index = 0; index < 100; index++) {
      String token = generator.generate();
      assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
      tokens.add(token);
    }

    assertThat(tokens).hasSize(100);
  }

  @Test
  void hashesTokensAsLowercaseSha256Hex() {
    String hash = new SharedRouteTokenHasher().hash("kivo");

    assertThat(hash)
        .isEqualTo("3d1ebb06f245bc2c40b6fa0b45d28d95260ccf441e606ccd84158dd0f9748843")
        .matches("[0-9a-f]{64}");
  }

  @Test
  void createsAReadablePngWithTheRequestedDimensions() throws Exception {
    byte[] png = new ZxingQrCodeGenerator().generatePng("https://routes.example/r/token", 256);

    assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
    Set<Integer> colors = new HashSet<>();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        colors.add(image.getRGB(x, y));
      }
    }
    assertThat(colors).hasSize(2);
  }
}
