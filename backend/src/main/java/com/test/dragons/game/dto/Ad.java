package com.test.dragons.game.dto;

public record Ad(
		String adId,
		String message,
		int reward,
		int expiresIn,
		Boolean encrypted,
		String probability) {
}
