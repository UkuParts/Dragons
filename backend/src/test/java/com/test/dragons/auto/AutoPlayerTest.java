package com.test.dragons.auto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.test.dragons.auto.dto.AutoMoveResult;
import com.test.dragons.auto.dto.AutoMoveResult.StopReason;
import com.test.dragons.game.GameService;
import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameState;
import com.test.dragons.game.dto.ShopItem;

class AutoPlayerTest {

	private static final List<ShopItem> SHOP = List.of(
			new ShopItem("hpot", "Healing potion", 50),
			new ShopItem("cs", "Cheap levels", 100),
			new ShopItem("ch", "Big levels", 300));

	private GameService games;
	private AutoPlayer player;

	@BeforeEach
	void setUp() {
		games = mock(GameService.class);
		player = new AutoPlayer(games);
	}

	private static Ad ad() {
		return new Ad("ad-1", "Fix a wagon", 10, 7, false, "Piece of cake");
	}

	private static GameState state(int lives, int gold, int turn) {
		return new GameState("game-1", lives, gold, 0, 0, 0, turn, List.of(ad()), SHOP, null, null, null,
				false);
	}

	private static GameState deadBoardState(int lives, int gold, int turn) {
		return new GameState("game-1", lives, gold, 0, 0, 0, turn, List.of(), SHOP, null, null, null, false);
	}

	@Test
	void buysTheBigLevelItemWhenItCanAffordTheReserve() {
		given(games.current("game-1")).willReturn(state(4, 450, 0));
		GameState after = state(4, 150, 1);
		given(games.buy("game-1", "ch")).willReturn(after);
		given(games.recordAutoMove("game-1", true, false)).willReturn(after);

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isFalse();
		assertThat(result.reason()).isNull();
		assertThat(result.state()).isEqualTo(after);
		verify(games).buy("game-1", "ch");
		verify(games).recordAutoMove("game-1", true, false);
		verify(games, never()).solve(any(), any());
	}

	@Test
	void solvesWhenItCannotAffordALevel() {
		given(games.current("game-1")).willReturn(state(3, 100, 5));
		GameState after = state(2, 110, 6);
		given(games.solve("game-1", "ad-1")).willReturn(after);
		given(games.recordAutoMove("game-1", false, false)).willReturn(after);

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isFalse();
		assertThat(result.state()).isEqualTo(after);
		verify(games).solve("game-1", "ad-1");
		verify(games).recordAutoMove("game-1", false, false);
	}

	@Test
	void waitsWhenThereIsNothingLeftToSolve() {
		given(games.current("game-1")).willReturn(deadBoardState(3, 0, 5));
		GameState after = deadBoardState(3, 0, 6);
		given(games.buy("game-1", AutoStrategy.SKIP_ITEM)).willReturn(after);
		given(games.recordAutoMove("game-1", false, true)).willReturn(after);

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isFalse();
		assertThat(result.reason()).isNull();
		assertThat(result.state()).isEqualTo(after);
		verify(games).buy("game-1", AutoStrategy.SKIP_ITEM);
		verify(games).recordAutoMove("game-1", false, true);
		verify(games, never()).solve(any(), any());
	}

	@Test
	void givesUpOnceTheSkipLimitIsReached() {
		given(games.current("game-1")).willReturn(deadBoardState(3, 0, 5));
		given(games.consecutiveWaits("game-1")).willReturn(AutoStrategy.MAX_SKIP);

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isTrue();
		assertThat(result.reason()).isEqualTo(StopReason.SKIP_LIMIT);
		verify(games, never()).buy(any(), any());
	}

	@Test
	void endsTheRunWhenTheBoardIsDeadAfterTheLevelingHorizon() {
		given(games.current("game-1")).willReturn(deadBoardState(3, 0, AutoStrategy.LEVELING_STOP_TURN));

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isTrue();
		assertThat(result.reason()).isEqualTo(StopReason.BOARD_DEAD);
		verify(games, never()).buy(any(), any());
	}

	@Test
	void finishesWhenTheGameIsAlreadyOver() {
		given(games.current("game-1")).willReturn(state(0, 100, 5));

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isTrue();
		assertThat(result.reason()).isEqualTo(StopReason.GAME_OVER);
		verify(games, never()).solve(any(), any());
	}

	@Test
	void reportsTheTurnLimit() {
		given(games.current("game-1")).willReturn(state(4, 100, AutoStrategy.RUN_END_TURN));

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isTrue();
		assertThat(result.reason()).isEqualTo(StopReason.TURN_LIMIT);
	}

	@Test
	void stopsBuyingLevelsAfterEightInARow() {
		given(games.current("game-1")).willReturn(state(4, 450, 0));
		given(games.purchaseStreak("game-1")).willReturn(0, 1, 2, 3, 4, 5, 6, 7, 8);
		given(games.buy("game-1", "ch")).willReturn(state(4, 450, 1));
		given(games.solve("game-1", "ad-1")).willReturn(state(4, 460, 1));
		given(games.recordAutoMove(any(), anyBoolean(), anyBoolean())).willReturn(state(4, 450, 1));

		for (int move = 0; move < 8; move++) {
			assertThat(player.nextMove("game-1").finished()).isFalse();
		}
		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isFalse();
		verify(games, times(8)).buy("game-1", "ch");
		verify(games).solve("game-1", "ad-1");
	}

	@Test
	void doesNotCountAFailedPurchaseTowardsTheBuyStreak() {
		given(games.current("game-1")).willReturn(state(4, 450, 0));
		given(games.buy("game-1", "ch")).willThrow(HttpClientErrorException.create(
				HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", HttpHeaders.EMPTY, new byte[0], null));
		given(games.recordStalledMove("game-1")).willReturn(state(4, 450, 0));

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isFalse();
		verify(games, never()).recordAutoMove(any(), anyBoolean(), anyBoolean());
		verify(games, never()).solve(any(), any());
	}

	@Test
	void givesUpAfterRepeatedMovesThatDoNotAdvanceTheTurn() {
		int[] waits = { 0 };
		given(games.current("game-1")).willReturn(state(4, 100, 5));
		given(games.solve("game-1", "ad-1")).willThrow(HttpClientErrorException.create(
				HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", HttpHeaders.EMPTY, new byte[0], null));
		given(games.recordStalledMove("game-1")).willAnswer(invocation -> {
			waits[0]++;
			return state(4, 100, 5);
		});
		given(games.consecutiveWaits("game-1")).willAnswer(invocation -> waits[0]);

		for (int move = 1; move < AutoStrategy.MAX_SKIP; move++) {
			assertThat(player.nextMove("game-1").finished()).isFalse();
		}
		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isTrue();
		assertThat(result.reason()).isEqualTo(StopReason.SKIP_LIMIT);
	}

	@Test
	void finishesWhenTheApiReturnsGone() {
		given(games.current("game-1")).willReturn(state(3, 100, 5)).willReturn(state(0, 100, 5));
		given(games.solve("game-1", "ad-1")).willThrow(HttpClientErrorException.create(HttpStatus.GONE,
				"Game Over", HttpHeaders.EMPTY, new byte[0], null));

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isTrue();
		assertThat(result.reason()).isEqualTo(StopReason.GAME_OVER);
		assertThat(result.state().lives()).isZero();
	}

	@Test
	void continuesAfterARecoverableActionFailure() {
		given(games.current("game-1")).willReturn(state(3, 100, 5)).willReturn(state(3, 100, 5));
		given(games.solve("game-1", "ad-1")).willThrow(HttpClientErrorException.create(
				HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", HttpHeaders.EMPTY,
				"{\"error\":\"The ad has expired.\"}".getBytes(), null));
		given(games.recordStalledMove("game-1")).willReturn(state(3, 100, 5));

		AutoMoveResult result = player.nextMove("game-1");

		assertThat(result.finished()).isFalse();
		assertThat(result.reason()).isNull();
		assertThat(result.state().lives()).isEqualTo(3);
		verify(games, never()).recordAutoMove(any(), anyBoolean(), anyBoolean());
	}

	@Test
	void propagatesServerFailuresForTheFrontendToRetry() {
		given(games.current("game-1")).willReturn(state(3, 100, 5));
		given(games.solve("game-1", "ad-1")).willThrow(HttpServerErrorException.create(
				HttpStatus.BAD_GATEWAY, "Bad Gateway", HttpHeaders.EMPTY, new byte[0], null));

		assertThatThrownBy(() -> player.nextMove("game-1"))
				.isInstanceOf(HttpServerErrorException.class);
	}
}
