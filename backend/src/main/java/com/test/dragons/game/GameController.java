package com.test.dragons.game;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.dragons.game.dto.GameState;

@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameService games;

	public GameController(GameService games) {
		this.games = games;
	}

	@PostMapping
	public GameState startGame() {
		return games.start();
	}

	@GetMapping("/{gameId}")
	public GameState getState(@PathVariable String gameId) {
		return games.current(gameId);
	}

	@PostMapping("/{gameId}/solve/{adId}")
	public GameState solveMessage(@PathVariable String gameId, @PathVariable String adId) {
		return games.solve(gameId, adId);
	}

	@PostMapping("/{gameId}/shop/buy/{itemId}")
	public GameState buyItem(@PathVariable String gameId, @PathVariable String itemId) {
		return games.buy(gameId, itemId);
	}

	@PostMapping("/{gameId}/investigate/reputation")
	public GameState investigateReputation(@PathVariable String gameId) {
		return games.investigateReputation(gameId);
	}
}
