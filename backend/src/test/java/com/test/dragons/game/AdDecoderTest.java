package com.test.dragons.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.test.dragons.game.dto.Ad;

class AdDecoderTest {

	private static String base64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void leavesPlainAdsUntouched() {
		Ad ad = new Ad("ad-1", "Fix a wagon", 10, 7, false, "Piece of cake");

		assertThat(AdDecoder.decode(ad)).isEqualTo(ad);
	}

	@Test
	void decodesBase64Fields() {
		Ad encrypted = new Ad(base64("abc123"),
				base64("Infiltrate The Ivory Pygmy Posse and recover their secrets."), 120, 3, true,
				base64("Quite likely"));

		Ad decoded = AdDecoder.decode(encrypted);

		assertThat(decoded.adId()).isEqualTo("abc123");
		assertThat(decoded.message()).isEqualTo("Infiltrate The Ivory Pygmy Posse and recover their secrets.");
		assertThat(decoded.probability()).isEqualTo("Quite likely");
		assertThat(decoded.reward()).isEqualTo(120);
	}

	@Test
	void fallsBackToRot13WhenBase64Fails() {
		Ad encrypted = new Ad(base64("ad-1"), base64("Uryy ..."), 10, 7, true, "Cvrpr bs pnxr");

		Ad decoded = AdDecoder.decode(encrypted);

		assertThat(decoded.probability()).isEqualTo("Piece of cake");
	}

	@Test
	void decodesUrlSafeBase64WithoutPadding() {
		String value = base64("Quite likely").replace("+", "-").replace("/", "_").replace("=", "");

		assertThat(AdDecoder.decodeField(value)).isEqualTo("Quite likely");
	}

	@Test
	void fallsBackToRot13WhenBase64DecodesToControlCharacters() {
		String encoded = Base64.getEncoder().encodeToString(new byte[] { 0x61, (byte) 0xc2, (byte) 0x85, 0x62 });

		assertThat(AdDecoder.decodeField(encoded)).isEqualTo("LpXSLt==");
	}

	@Test
	void keepsDecodedValuesWithLineBreaksOrJoiners() {
		assertThat(AdDecoder.decodeField(base64("First line\nSecond line")))
				.isEqualTo("First line\nSecond line");
		assertThat(AdDecoder.decodeField(
				Base64.getEncoder().encodeToString(new byte[] { 0x61, (byte) 0xe2, (byte) 0x80, (byte) 0x8d, 0x62 })))
				.isEqualTo("a\u200db");
	}
}
