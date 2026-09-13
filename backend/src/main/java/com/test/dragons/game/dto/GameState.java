package com.test.dragons.game.dto;

import java.util.List;

public record GameState(
		String gameId,
		int lives,
		int gold,
		int level,
		int score,
		int highScore,
		int turn,
		List<Ad> tasks,
		List<ShopItem> shopItems,
		Reputation reputation,
		Integer reputationTurn,
		String lastMessage,
		boolean lastMessageFailed) {
}
