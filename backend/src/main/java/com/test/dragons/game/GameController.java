package com.test.dragons.game;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;
import com.test.dragons.game.dto.PurchaseResult;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;
import com.test.dragons.game.dto.SolveResult;

@RestController
@RequestMapping("/api/games")
public class GameController {

	private final DragonsApiClient dragonsApi;

	public GameController(DragonsApiClient dragonsApi) {
		this.dragonsApi = dragonsApi;
	}

	@PostMapping
	public GameStart startGame() {
		return dragonsApi.startGame();
	}

	@GetMapping("/{gameId}/messages")
	public List<Ad> getMessages(@PathVariable String gameId) {
		return dragonsApi.getMessages(gameId);
	}

	@PostMapping("/{gameId}/solve/{adId}")
	public SolveResult solveMessage(@PathVariable String gameId, @PathVariable String adId) {
		return dragonsApi.solveMessage(gameId, adId);
	}

	@GetMapping("/{gameId}/shop")
	public List<ShopItem> getShopItems(@PathVariable String gameId) {
		return dragonsApi.getShopItems(gameId);
	}

	@PostMapping("/{gameId}/shop/buy/{itemId}")
	public PurchaseResult buyItem(@PathVariable String gameId, @PathVariable String itemId) {
		return dragonsApi.buyItem(gameId, itemId);
	}

	@PostMapping("/{gameId}/investigate/reputation")
	public Reputation investigateReputation(@PathVariable String gameId) {
		return dragonsApi.investigateReputation(gameId);
	}
}
