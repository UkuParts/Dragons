package com.test.dragons.game;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;
import com.test.dragons.game.dto.GameState;
import com.test.dragons.game.dto.PurchaseResult;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;
import com.test.dragons.game.dto.SolveResult;

@Service
public class GameService {

	private final DragonsApiClient dragonsApi;
	private ActiveGame active;

	public GameService(DragonsApiClient dragonsApi) {
		this.dragonsApi = dragonsApi;
	}

	public synchronized GameState start() {
		GameStart start = dragonsApi.startGame();
		ActiveGame game = new ActiveGame(start);
		active = game;
		refreshBestEffort(game);
		return game.snapshot();
	}

	public synchronized GameState current(String gameId) {
		return require(gameId).snapshot();
	}

	public synchronized GameState solve(String gameId, String adId) {
		ActiveGame game = require(gameId);
		try {
			SolveResult result = dragonsApi.solveMessage(gameId, adId);
			game.applySolve(result);
			if (result.lives() > 0) {
				refreshBestEffort(game);
			}
		} catch (RestClientResponseException exception) {
			recover(game, exception);
			throw exception;
		}
		return game.snapshot();
	}

	public synchronized GameState buy(String gameId, String itemId) {
		ActiveGame game = require(gameId);
		try {
			PurchaseResult result = dragonsApi.buyItem(gameId, itemId);
			game.applyPurchase(result, itemId);
			if (result.lives() > 0) {
				refreshBestEffort(game);
			}
		} catch (RestClientResponseException exception) {
			recover(game, exception);
			throw exception;
		}
		return game.snapshot();
	}

	public synchronized GameState investigateReputation(String gameId) {
		ActiveGame game = require(gameId);
		try {
			Reputation reputation = dragonsApi.investigateReputation(gameId);
			game.applyInvestigation(reputation);
			refreshBestEffort(game);
		} catch (RestClientResponseException exception) {
			recover(game, exception);
			throw exception;
		}
		return game.snapshot();
	}

	public synchronized int purchaseStreak(String gameId) {
		return require(gameId).purchaseStreak();
	}

	public synchronized int consecutiveWaits(String gameId) {
		return require(gameId).consecutiveWaits();
	}

	public synchronized GameState recordAutoMove(String gameId, boolean levelPurchase, boolean skip) {
		ActiveGame game = require(gameId);
		game.recordAutoMove(levelPurchase, skip);
		return game.snapshot();
	}

	public synchronized GameState recordStalledMove(String gameId) {
		ActiveGame game = require(gameId);
		game.recordStalledMove();
		return game.snapshot();
	}

	private void refresh(ActiveGame game) {
		List<Ad> tasks = dragonsApi.getMessages(game.id()).stream().map(AdDecoder::decode).toList();
		List<ShopItem> shopItems = dragonsApi.getShopItems(game.id());
		game.setBoard(tasks, shopItems);
	}

	private ActiveGame require(String gameId) {
		if (active == null || !active.id().equals(gameId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return active;
	}

	private void recover(ActiveGame game, RestClientResponseException exception) {
		if (exception.getStatusCode().isSameCodeAs(HttpStatus.GONE)) {
			game.markGameOver();
			return;
		}
		refreshBestEffort(game);
	}

	private void refreshBestEffort(ActiveGame game) {
		try {
			refresh(game);
		} catch (RuntimeException ignored) {
			// the next action or GET reloads the free board and shop
		}
	}
}
