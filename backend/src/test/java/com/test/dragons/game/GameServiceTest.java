package com.test.dragons.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;
import com.test.dragons.game.dto.GameState;
import com.test.dragons.game.dto.PurchaseResult;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;
import com.test.dragons.game.dto.SolveResult;

class GameServiceTest {

	private DragonsApiClient dragonsApi;
	private GameService games;

	@BeforeEach
	void setUp() {
		dragonsApi = mock(DragonsApiClient.class);
		games = new GameService(dragonsApi);
	}

	private static Ad ad(String id) {
		return new Ad(id, "Fix a wagon", 10, 7, false, "Piece of cake");
	}

	private static String base64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private void givenRunningGame() {
		given(dragonsApi.startGame()).willReturn(new GameStart("game-1", 3, 0, 0, 0, 0, 0));
		given(dragonsApi.getMessages("game-1")).willReturn(List.of(ad("ad-1")));
		given(dragonsApi.getShopItems("game-1"))
				.willReturn(List.of(new ShopItem("hpot", "Healing potion", 50)));
		games.start();
	}

	@Test
	void startsAndStoresAGame() {
		given(dragonsApi.startGame()).willReturn(new GameStart("game-1", 3, 0, 0, 0, 0, 0));
		given(dragonsApi.getMessages("game-1"))
				.willReturn(List.of(new Ad("ad-1", "Fix a wagon", 10, 7, false, "Piece of cake")));
		given(dragonsApi.getShopItems("game-1"))
				.willReturn(List.of(new ShopItem("hpot", "Healing potion", 50)));

		GameState state = games.start();

		assertThat(state.gameId()).isEqualTo("game-1");
		assertThat(state.lives()).isEqualTo(3);
		assertThat(state.tasks()).extracting(Ad::adId).containsExactly("ad-1");
		assertThat(state.shopItems()).extracting(ShopItem::id).containsExactly("hpot");
		assertThat(games.current("game-1")).isEqualTo(state);
	}

	@Test
	void decodesTheBoardBeforeStoringIt() {
		given(dragonsApi.startGame()).willReturn(new GameStart("game-1", 3, 0, 0, 0, 0, 0));
		given(dragonsApi.getMessages("game-1"))
				.willReturn(List.of(new Ad(base64("ad1"),
						base64("Infiltrate The Ivory Pygmy Posse and recover their secrets."), 120, 3, true,
						base64("Quite likely"))));
		given(dragonsApi.getShopItems("game-1")).willReturn(List.of());

		GameState state = games.start();

		assertThat(state.tasks().getFirst().adId()).isEqualTo("ad1");
		assertThat(state.tasks().getFirst().probability()).isEqualTo("Quite likely");
	}

	@Test
	void solvesAndRefreshesTheBoard() {
		givenRunningGame();
		given(dragonsApi.solveMessage("game-1", "ad-1"))
				.willReturn(new SolveResult(true, 3, 10, 10, 0, 1, "You successfully solved the mission!"));

		GameState state = games.solve("game-1", "ad-1");

		assertThat(state.gold()).isEqualTo(10);
		assertThat(state.score()).isEqualTo(10);
		assertThat(state.turn()).isEqualTo(1);
		assertThat(state.lastMessage()).isEqualTo("You successfully solved the mission!");
		assertThat(state.lastMessageFailed()).isFalse();
		assertThat(state.tasks()).extracting(Ad::adId).containsExactly("ad-1");
	}

	@Test
	void buysAndReportsAFailedPurchase() {
		givenRunningGame();
		given(dragonsApi.buyItem("game-1", "hpot"))
				.willReturn(new PurchaseResult(false, 0, 3, 0, 1));

		GameState state = games.buy("game-1", "hpot");

		assertThat(state.turn()).isEqualTo(1);
		assertThat(state.lastMessage()).isEqualTo("Could not buy Healing potion.");
		assertThat(state.lastMessageFailed()).isTrue();
	}

	@Test
	void investigatesAndAdvancesTheTurn() {
		givenRunningGame();
		given(dragonsApi.investigateReputation("game-1")).willReturn(new Reputation(1.5, 0, -2));

		GameState state = games.investigateReputation("game-1");

		assertThat(state.turn()).isEqualTo(1);
		assertThat(state.reputationTurn()).isEqualTo(1);
		assertThat(state.reputation()).isEqualTo(new Reputation(1.5, 0, -2));
	}

	@Test
	void rejectsGamesThatAreNotActive() {
		assertThatThrownBy(() -> games.current("missing")).isInstanceOf(ResponseStatusException.class);
		assertThatThrownBy(() -> games.solve("missing", "ad-1"))
				.isInstanceOf(ResponseStatusException.class);

		givenRunningGame();

		assertThatThrownBy(() -> games.current("other")).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void marksTheGameOverWhenTheApiReturnsGone() {
		givenRunningGame();
		given(dragonsApi.solveMessage("game-1", "ad-1")).willThrow(HttpClientErrorException
				.create(HttpStatus.GONE, "Game Over", HttpHeaders.EMPTY, new byte[0], null));

		assertThatThrownBy(() -> games.solve("game-1", "ad-1"))
				.isInstanceOf(HttpClientErrorException.class);
		assertThat(games.current("game-1").lives()).isZero();
	}

	@Test
	void refreshesTheBoardAfterARecoverableActionFailure() {
		given(dragonsApi.startGame()).willReturn(new GameStart("game-1", 3, 0, 0, 0, 0, 0));
		given(dragonsApi.getMessages("game-1"))
				.willReturn(List.of(ad("ad-1")))
				.willReturn(List.of(ad("ad-2")));
		given(dragonsApi.getShopItems("game-1")).willReturn(List.of());
		games.start();
		given(dragonsApi.solveMessage("game-1", "ad-1")).willThrow(HttpClientErrorException
				.create(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", HttpHeaders.EMPTY,
						"{\"error\":\"The ad has expired.\"}".getBytes(), null));

		assertThatThrownBy(() -> games.solve("game-1", "ad-1"))
				.isInstanceOf(HttpClientErrorException.class);
		assertThat(games.current("game-1").tasks()).extracting(Ad::adId).containsExactly("ad-2");
	}

	@Test
	void keepsTheStartedGameWhenTheBoardCannotBeLoaded() {
		given(dragonsApi.startGame()).willReturn(new GameStart("game-1", 3, 0, 0, 0, 0, 0));
		given(dragonsApi.getMessages("game-1")).willThrow(new RestClientException("Connection refused"));

		GameState state = games.start();

		assertThat(state.gameId()).isEqualTo("game-1");
		assertThat(state.tasks()).isEmpty();
		assertThat(games.current("game-1").lives()).isEqualTo(3);
	}

	@Test
	void recordsAndResetsAutomaticMoveCounters() {
		givenRunningGame();

		assertThat(games.purchaseStreak("game-1")).isZero();
		assertThat(games.consecutiveWaits("game-1")).isZero();

		games.recordAutoMove("game-1", true, false);
		games.recordAutoMove("game-1", true, false);
		assertThat(games.purchaseStreak("game-1")).isEqualTo(2);

		GameState waited = games.recordAutoMove("game-1", false, true);
		assertThat(games.purchaseStreak("game-1")).isZero();
		assertThat(games.consecutiveWaits("game-1")).isEqualTo(1);
		assertThat(waited.lastMessage()).isEqualTo("Waiting for a solvable mission.");
		assertThat(waited.lastMessageFailed()).isFalse();

		games.recordStalledMove("game-1");
		assertThat(games.consecutiveWaits("game-1")).isEqualTo(2);

		games.recordAutoMove("game-1", false, false);
		assertThat(games.consecutiveWaits("game-1")).isZero();
	}

	@Test
	void resetsAutomaticMoveCountersForANewGame() {
		givenRunningGame();
		games.recordAutoMove("game-1", true, false);
		games.recordAutoMove("game-1", false, true);

		given(dragonsApi.startGame()).willReturn(new GameStart("game-2", 3, 0, 0, 0, 0, 0));
		given(dragonsApi.getMessages("game-2")).willReturn(List.of(ad("ad-1")));
		given(dragonsApi.getShopItems("game-2")).willReturn(List.of());
		games.start();

		assertThat(games.purchaseStreak("game-2")).isZero();
		assertThat(games.consecutiveWaits("game-2")).isZero();
	}
}
