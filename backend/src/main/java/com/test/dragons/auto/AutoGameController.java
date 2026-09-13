package com.test.dragons.auto;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.dragons.auto.dto.AutoMoveResult;

@RestController
@RequestMapping("/api/auto/games")
public class AutoGameController {

	private final AutoPlayer autoPlayer;

	public AutoGameController(AutoPlayer autoPlayer) {
		this.autoPlayer = autoPlayer;
	}

	@PostMapping("/{gameId}/next-move")
	public AutoMoveResult nextMove(@PathVariable String gameId) {
		return autoPlayer.nextMove(gameId);
	}
}
