package com.test.dragons.game;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameState;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;

@WebMvcTest(GameController.class)
class GameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameService games;

	private static GameState state() {
		return new GameState("game-1", 3, 0, 0, 0, 0, 0,
				List.of(new Ad("ad-1", "Fix a wagon", 10, 7, true, "Piece of cake")),
				List.of(new ShopItem("hpot", "Healing potion", 50)), null, null, null, false);
	}

	@Test
	void startsANewGame() throws Exception {
		given(games.start()).willReturn(state());

		mockMvc.perform(post("/api/games"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameId").value("game-1"))
				.andExpect(jsonPath("$.lives").value(3))
				.andExpect(jsonPath("$.tasks[0].adId").value("ad-1"))
				.andExpect(jsonPath("$.shopItems[0].id").value("hpot"));
	}

	@Test
	void returnsTheStoredState() throws Exception {
		given(games.current("game-1")).willReturn(state());

		mockMvc.perform(get("/api/games/game-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameId").value("game-1"));
	}

	@Test
	void solvesAMessage() throws Exception {
		given(games.solve("game-1", "ad-1")).willReturn(state());

		mockMvc.perform(post("/api/games/game-1/solve/ad-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameId").value("game-1"));

		verify(games).solve("game-1", "ad-1");
	}

	@Test
	void buysAnItem() throws Exception {
		given(games.buy("game-1", "hpot")).willReturn(state());

		mockMvc.perform(post("/api/games/game-1/shop/buy/hpot"))
				.andExpect(status().isOk());

		verify(games).buy("game-1", "hpot");
	}

	@Test
	void investigatesReputation() throws Exception {
		given(games.investigateReputation("game-1")).willReturn(new GameState("game-1", 3, 0, 0, 0, 0, 1,
				List.of(), List.of(), new Reputation(1.5, 0, -2), 1, null, false));

		mockMvc.perform(post("/api/games/game-1/investigate/reputation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reputation.people").value(1.5))
				.andExpect(jsonPath("$.reputationTurn").value(1));
	}

	@Test
	void mapsUnknownGamesToNotFound() throws Exception {
		given(games.current("missing")).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		mockMvc.perform(get("/api/games/missing"))
				.andExpect(status().isNotFound());
	}

	@Test
	void mapsGameOverToGone() throws Exception {
		given(games.solve("game-1", "ad-1")).willThrow(HttpClientErrorException.create(HttpStatus.GONE,
				"Game Over", HttpHeaders.EMPTY,
				"{\"status\":\"Game Over\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

		mockMvc.perform(post("/api/games/game-1/solve/ad-1"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.error").value("Game Over"));
	}

	@Test
	void forwardsRejectedActionMessages() throws Exception {
		given(games.solve("game-1", "ad-1")).willThrow(HttpClientErrorException.create(
				HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", HttpHeaders.EMPTY,
				"{\"error\":\"The ad has expired.\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

		mockMvc.perform(post("/api/games/game-1/solve/ad-1"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("The ad has expired."));
	}

	@Test
	void mapsTransportFailuresToBadGateway() throws Exception {
		given(games.current("game-1")).willThrow(new RestClientException("Connection refused"));

		mockMvc.perform(get("/api/games/game-1"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void forwardsRetryAfterWhenRateLimited() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, "7");
		given(games.current("game-1")).willThrow(HttpClientErrorException.create(
				HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], null));

		mockMvc.perform(get("/api/games/game-1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "7"));
	}
}
