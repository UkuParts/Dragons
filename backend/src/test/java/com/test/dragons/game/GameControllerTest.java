package com.test.dragons.game;

import static org.mockito.ArgumentMatchers.eq;
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

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;
import com.test.dragons.game.dto.PurchaseResult;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;
import com.test.dragons.game.dto.SolveResult;

@WebMvcTest(GameController.class)
class GameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DragonsApiClient dragonsApi;

	@Test
	void startsANewGame() throws Exception {
		given(dragonsApi.startGame()).willReturn(new GameStart("game-1", 3, 0, 0, 0, 0, 0));

		mockMvc.perform(post("/api/games"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameId").value("game-1"))
				.andExpect(jsonPath("$.lives").value(3));
	}

	@Test
	void listsMessages() throws Exception {
		given(dragonsApi.getMessages("game-1"))
				.willReturn(List.of(new Ad("ad-1", "Fix a wagon", 10, 7, true, "Piece of cake")));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].adId").value("ad-1"))
				.andExpect(jsonPath("$[0].encrypted").value(true))
				.andExpect(jsonPath("$[0].probability").value("Piece of cake"));
	}

	@Test
	void solvesAMessage() throws Exception {
		given(dragonsApi.solveMessage("game-1", "ad-1"))
				.willReturn(new SolveResult(true, 3, 10, 10, 0, 1, "You successfully solved the mission!"));

		mockMvc.perform(post("/api/games/game-1/solve/ad-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.gold").value(10));

		verify(dragonsApi).solveMessage("game-1", "ad-1");
	}

	@Test
	void listsShopItems() throws Exception {
		given(dragonsApi.getShopItems("game-1"))
				.willReturn(List.of(new ShopItem("hpot", "Healing potion", 50)));

		mockMvc.perform(get("/api/games/game-1/shop"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("hpot"))
				.andExpect(jsonPath("$[0].cost").value(50));
	}

	@Test
	void buysAnItem() throws Exception {
		given(dragonsApi.buyItem("game-1", "hpot"))
				.willReturn(new PurchaseResult(true, 0, 4, 0, 5));

		mockMvc.perform(post("/api/games/game-1/shop/buy/hpot"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shoppingSuccess").value(true))
				.andExpect(jsonPath("$.lives").value(4));

		verify(dragonsApi).buyItem(eq("game-1"), eq("hpot"));
	}

	@Test
	void investigatesReputation() throws Exception {
		given(dragonsApi.investigateReputation("game-1"))
				.willReturn(new Reputation(1.5, 0, -2));

		mockMvc.perform(post("/api/games/game-1/investigate/reputation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.people").value(1.5))
				.andExpect(jsonPath("$.underworld").value(-2));
	}

	@Test
	void mapsUnknownGameToNotFound() throws Exception {
		given(dragonsApi.getMessages("missing"))
				.willThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY,
						new byte[0], null));

		mockMvc.perform(get("/api/games/missing/messages"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void mapsGameOverToGone() throws Exception {
		given(dragonsApi.getMessages("game-1"))
				.willThrow(HttpClientErrorException.create(HttpStatus.GONE, "Game Over", HttpHeaders.EMPTY,
						"{\"status\":\"Game Over\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.error").value("Game Over"));
	}

	@Test
	void forwardsRejectedActionMessages() throws Exception {
		given(dragonsApi.solveMessage("game-1", "ad-1"))
				.willThrow(HttpClientErrorException.create(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity",
						HttpHeaders.EMPTY, "{\"error\":\"The ad has expired.\"}".getBytes(StandardCharsets.UTF_8),
						StandardCharsets.UTF_8));

		mockMvc.perform(post("/api/games/game-1/solve/ad-1"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("The ad has expired."));
	}

	@Test
	void usesGenericMessageForNonJsonErrors() throws Exception {
		given(dragonsApi.getMessages("game-1"))
				.willThrow(HttpClientErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", HttpHeaders.EMPTY,
						"<html>no json here</html>".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").value("The Dragons of Mugloar API rejected the request."));
	}

	@Test
	void mapsRateLimitingToTooManyRequests() throws Exception {
		given(dragonsApi.getMessages("game-1"))
				.willThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
						HttpHeaders.EMPTY, new byte[0], null));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void forwardsRetryAfterWhenRateLimited() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, "7");
		given(dragonsApi.getMessages("game-1"))
				.willThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
						headers, new byte[0], null));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "7"));
	}

	@Test
	void forwardsUnexpectedClientErrors() throws Exception {
		given(dragonsApi.getMessages("game-1"))
				.willThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY,
						"{\"error\":\"Blocked by the game API.\"}".getBytes(StandardCharsets.UTF_8),
						StandardCharsets.UTF_8));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Blocked by the game API."));
	}

	@Test
	void mapsTransportFailuresToBadGateway() throws Exception {
		given(dragonsApi.getMessages("game-1")).willThrow(new RestClientException("Connection refused"));

		mockMvc.perform(get("/api/games/game-1/messages"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").exists())
				.andExpect(jsonPath("$.detail").doesNotExist());
	}
}
