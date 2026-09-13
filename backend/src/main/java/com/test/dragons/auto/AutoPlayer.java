package com.test.dragons.auto;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import com.test.dragons.auto.AutoStrategy.Action;
import com.test.dragons.auto.AutoStrategy.Buy;
import com.test.dragons.auto.AutoStrategy.Skip;
import com.test.dragons.auto.AutoStrategy.Solve;
import com.test.dragons.auto.dto.AutoMoveResult;
import com.test.dragons.auto.dto.AutoMoveResult.StopReason;
import com.test.dragons.game.GameService;
import com.test.dragons.game.dto.GameState;

/**
 * Executes one policy move at a time. The frontend owns the pacing; the game state lives in
 * {@link GameService}.
 */
@Service
public class AutoPlayer {

	private static final Logger log = LoggerFactory.getLogger(AutoPlayer.class);

	private final GameService games;

	public AutoPlayer(GameService games) {
		this.games = games;
	}

	public synchronized AutoMoveResult nextMove(String gameId) {
		GameState state = games.current(gameId);
		if (state.lives() <= 0) {
			return new AutoMoveResult(true, StopReason.GAME_OVER, state);
		}
		if (state.turn() >= AutoStrategy.RUN_END_TURN) {
			return new AutoMoveResult(true, StopReason.TURN_LIMIT, state);
		}

		Action action = AutoStrategy.decide(state.lives(), state.gold(), state.turn(),
				games.purchaseStreak(gameId), state.tasks(), state.shopItems());
		if (action instanceof Skip) {
			return skip(gameId, state);
		}
		if (action instanceof Solve solve) {
			return move(gameId, state, () -> games.solve(gameId, solve.adId()), false);
		}

		Buy buy = (Buy) action;
		return move(gameId, state, () -> games.buy(gameId, buy.itemId()),
				AutoStrategy.isLevelItem(buy.itemId()));
	}

	private AutoMoveResult move(String gameId, GameState state, Supplier<GameState> action,
			boolean levelPurchase) {
		GameState next = execute(gameId, action);
		if (next.lives() <= 0) {
			return result(next);
		}
		if (next.turn() > state.turn()) {
			return result(games.recordAutoMove(gameId, levelPurchase, false));
		}
		GameState waited = games.recordStalledMove(gameId);
		return games.consecutiveWaits(gameId) >= AutoStrategy.MAX_SKIP
				? new AutoMoveResult(true, StopReason.SKIP_LIMIT, waited)
				: result(waited);
	}

	private AutoMoveResult skip(String gameId, GameState state) {
		// The board is effectively all Impossible once levelling stops.
		if (state.turn() >= AutoStrategy.LEVELING_STOP_TURN) {
			return new AutoMoveResult(true, StopReason.BOARD_DEAD, state);
		}
		if (games.consecutiveWaits(gameId) >= AutoStrategy.MAX_SKIP) {
			return new AutoMoveResult(true, StopReason.SKIP_LIMIT, state);
		}
		execute(gameId, () -> games.buy(gameId, AutoStrategy.SKIP_ITEM));
		return result(games.recordAutoMove(gameId, false, true));
	}

	private GameState execute(String gameId, Supplier<GameState> action) {
		try {
			return action.get();
		} catch (RestClientResponseException exception) {
			if (!exception.getStatusCode().is4xxClientError()) {
				throw exception;
			}
			log.warn("Automatic move on game {} was rejected with {}, continuing with the refreshed state",
					gameId, exception.getStatusCode());
			return games.current(gameId);
		}
	}

	private AutoMoveResult result(GameState state) {
		return state.lives() <= 0
				? new AutoMoveResult(true, StopReason.GAME_OVER, state)
				: new AutoMoveResult(false, null, state);
	}
}
