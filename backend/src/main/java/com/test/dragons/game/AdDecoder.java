package com.test.dragons.game;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import com.test.dragons.game.dto.Ad;

/**
 * Encrypted ads carry base64 fields with a rot13 fallback; every board stored in the game state is
 * decoded before it is used or returned.
 */
final class AdDecoder {

	private static final Pattern CONTROL_CHARS = Pattern
			.compile("[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f\\u007f-\\u009f]");

	private AdDecoder() {
	}

	static Ad decode(Ad ad) {
		if (!Boolean.TRUE.equals(ad.encrypted())) {
			return ad;
		}
		return new Ad(decodeField(ad.adId()), decodeField(ad.message()), ad.reward(), ad.expiresIn(),
				ad.encrypted(), decodeField(ad.probability()));
	}

	static String decodeField(String value) {
		String decoded = fromBase64(value);
		return decoded != null ? decoded : rot13(value);
	}

	private static String fromBase64(String value) {
		try {
			String standard = value.replace('-', '+').replace('_', '/');
			int remainder = standard.length() % 4;
			String padded = remainder == 0 ? standard : standard + "=".repeat(4 - remainder);
			byte[] bytes = Base64.getDecoder().decode(padded);
			String decoded = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes))
					.toString();
			return CONTROL_CHARS.matcher(decoded).find() ? null : decoded;
		} catch (IllegalArgumentException | CharacterCodingException exception) {
			return null;
		}
	}

	private static String rot13(String value) {
		StringBuilder result = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character >= 'a' && character <= 'z') {
				result.append((char) ('a' + (character - 'a' + 13) % 26));
			} else if (character >= 'A' && character <= 'Z') {
				result.append((char) ('A' + (character - 'A' + 13) % 26));
			} else {
				result.append(character);
			}
		}
		return result.toString();
	}
}
