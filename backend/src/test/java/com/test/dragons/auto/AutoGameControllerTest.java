package com.test.dragons.auto;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.test.dragons.auto.dto.AutoMoveResult;
import com.test.dragons.auto.dto.AutoMoveResult.StopReason;
import com.test.dragons.game.dto.GameState;

@WebMvcTest(AutoGameController.class)
class AutoGameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AutoPlayer autoPlayer;

	private static AutoMoveResult result(boolean finished, StopReason reason) {
		return new AutoMoveResult(finished, reason, new GameState("game-1", 3, 0, 0, 0, 0, 1, List.of(),
				List.of(), null, null, "You successfully solved the mission!", false));
	}

	@Test
	void requestsTheNextMove() throws Exception {
		given(autoPlayer.nextMove("game-1")).willReturn(result(false, null));

		mockMvc.perform(post("/api/auto/games/game-1/next-move"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.finished").value(false))
				.andExpect(jsonPath("$.reason").isEmpty())
				.andExpect(jsonPath("$.state.gameId").value("game-1"))
				.andExpect(jsonPath("$.state.lastMessage").value("You successfully solved the mission!"));

		verify(autoPlayer).nextMove("game-1");
	}

	@Test
	void reportsAFinishedRun() throws Exception {
		given(autoPlayer.nextMove("game-1")).willReturn(result(true, StopReason.GAME_OVER));

		mockMvc.perform(post("/api/auto/games/game-1/next-move"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.finished").value(true))
				.andExpect(jsonPath("$.reason").value("GAME_OVER"));
	}

	@Test
	void mapsUnknownGamesToNotFound() throws Exception {
		given(autoPlayer.nextMove("missing")).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		mockMvc.perform(post("/api/auto/games/missing/next-move"))
				.andExpect(status().isNotFound());
	}
}
