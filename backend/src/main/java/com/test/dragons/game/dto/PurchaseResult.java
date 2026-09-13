package com.test.dragons.game.dto;

public record PurchaseResult(
		boolean shoppingSuccess,
		int gold,
		int lives,
		int level,
		int turn) {
}
