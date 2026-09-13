package com.test.dragons.game;

import java.util.List;
import java.util.Optional;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;
import com.test.dragons.game.dto.GameState;
import com.test.dragons.game.dto.PurchaseResult;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;
import com.test.dragons.game.dto.SolveResult;

final class ActiveGame {

	private final String gameId;
	private int lives;
	private int gold;
	private int level;
	private int score;
	private int highScore;
	private int turn;
	private List<Ad> tasks = List.of();
	private List<ShopItem> shopItems = List.of();
	private Reputation reputation;
	private Integer reputationTurn;
	private String lastMessage;
	private boolean lastMessageFailed;
	private int purchaseStreak;
	private int consecutiveWaits;

	ActiveGame(GameStart start) {
		this.gameId = start.gameId();
		this.lives = start.lives();
		this.gold = start.gold();
		this.level = start.level();
		this.score = start.score();
		this.highScore = start.highScore();
		this.turn = start.turn();
	}

	String id() {
		return gameId;
	}

	GameState snapshot() {
		return new GameState(gameId, lives, gold, level, score, highScore, turn, List.copyOf(tasks),
				List.copyOf(shopItems), reputation, reputationTurn, lastMessage, lastMessageFailed);
	}

	void setBoard(List<Ad> tasks, List<ShopItem> shopItems) {
		this.tasks = tasks;
		this.shopItems = shopItems;
	}

	void applySolve(SolveResult result) {
		this.lives = result.lives();
		this.gold = result.gold();
		this.score = result.score();
		this.highScore = result.highScore();
		this.turn = result.turn();
		this.lastMessage = result.message();
		this.lastMessageFailed = !result.success();
	}

	void applyPurchase(PurchaseResult result, String itemId) {
		this.gold = result.gold();
		this.lives = result.lives();
		this.level = result.level();
		this.turn = result.turn();
		if (result.shoppingSuccess()) {
			this.lastMessage = itemName(itemId)
					.map(name -> "Bought " + name + ".")
					.orElse("The purchase succeeded.");
		} else {
			this.lastMessage = itemName(itemId)
					.map(name -> "Could not buy " + name + ".")
					.orElse("The purchase failed.");
		}
		this.lastMessageFailed = !result.shoppingSuccess();
	}

	void applyInvestigation(Reputation reputation) {
		this.reputation = reputation;
		this.turn++;
		this.reputationTurn = this.turn;
	}

	void markGameOver() {
		this.lives = 0;
	}

	int purchaseStreak() {
		return purchaseStreak;
	}

	int consecutiveWaits() {
		return consecutiveWaits;
	}

	void recordAutoMove(boolean levelPurchase, boolean skip) {
		if (skip) {
			consecutiveWaits++;
			purchaseStreak = 0;
			this.lastMessage = "Waiting for a solvable mission.";
			this.lastMessageFailed = false;
		} else if (levelPurchase) {
			purchaseStreak++;
			consecutiveWaits = 0;
		} else {
			purchaseStreak = 0;
			consecutiveWaits = 0;
		}
	}

	void recordStalledMove() {
		consecutiveWaits++;
	}

	private Optional<String> itemName(String itemId) {
		return shopItems.stream()
				.filter(item -> item.id().equals(itemId))
				.map(ShopItem::name)
				.findFirst();
	}
}
