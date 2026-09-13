package com.test.dragons.auto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.ShopItem;

public final class AutoStrategy {

	public static final int LEVELING_STOP_TURN = 115;
	public static final int RUN_END_TURN = 175;
	public static final int GOLD_RESERVE = 150;
	public static final int MAX_PURCHASE_STREAK = 8;
	public static final int MAX_SKIP = 25;
	public static final int EMERGENCY_HEAL_LIVES = 2;
	public static final int HEAL_COST = 50;
	public static final int SUICIDE_MISSION_MIN_TURN = 30;

	/** Buying an unknown item id still consumes a turn, which is the only way to wait. */
	public static final String SKIP_ITEM = "zzz";

	private static final String HEIST_PREFIX = "Steal super awesome";
	private static final String HEAL_ITEM = "hpot";
	private static final String SUICIDE_MISSION = "Suicide mission";

	private static final List<String> BIG_LEVEL_ITEMS = List.of("ch", "rf", "iron", "mtrix", "wingpotmax");
	private static final List<String> SMALL_LEVEL_ITEMS = List.of("cs", "gas", "wax", "tricks", "wingpot");

	private static final Map<String, Double> SUCCESS_PROBABILITY = Map.ofEntries(
			Map.entry("Sure thing", 0.983),
			Map.entry("Piece of cake", 0.946),
			Map.entry("Walk in the park", 0.854),
			Map.entry("Quite likely", 0.737),
			Map.entry("Hmmm....", 0.666),
			Map.entry("Gamble", 0.561),
			Map.entry("Risky", 0.485),
			Map.entry("Rather detrimental", 0.371),
			Map.entry("Playing with fire", 0.3),
			Map.entry("Suicide mission", 0.092),
			Map.entry("Impossible", 0.0));

	private AutoStrategy() {
	}

	public sealed interface Action permits Solve, Buy, Skip {
	}

	public record Solve(String adId) implements Action {
	}

	public record Buy(String itemId) implements Action {
	}

	public record Skip() implements Action {
	}

	public static Action decide(int lives, int gold, int turn, int purchaseStreak, List<Ad> ads,
			List<ShopItem> items) {
		Optional<ShopItem> healItem = findItem(items, HEAL_ITEM);

		if (lives < EMERGENCY_HEAL_LIVES && healItem.isPresent() && gold >= HEAL_COST) {
			return new Buy(HEAL_ITEM);
		}

		if (turn < LEVELING_STOP_TURN && purchaseStreak < MAX_PURCHASE_STREAK) {
			Optional<ShopItem> big = findAffordable(items, BIG_LEVEL_ITEMS, gold, GOLD_RESERVE);
			if (big.isPresent()) {
				return new Buy(big.get().id());
			}
			Optional<ShopItem> small = findAffordable(items, SMALL_LEVEL_ITEMS, gold, GOLD_RESERVE);
			if (small.isPresent()) {
				return new Buy(small.get().id());
			}
		}

		Ad best = pickTask(ads, turn);
		return best == null ? new Skip() : new Solve(best.adId());
	}

	public static Ad pickTask(List<Ad> ads, int turn) {
		Ad best = null;
		double bestValue = 0;
		for (Ad ad : ads) {
			if (isHeist(ad)) {
				continue;
			}
			if (turn < SUICIDE_MISSION_MIN_TURN && SUICIDE_MISSION.equals(ad.probability())) {
				continue;
			}
			double value = successProbability(ad.probability()) * ad.reward();
			if (value > bestValue) {
				best = ad;
				bestValue = value;
			}
		}
		return best;
	}

	public static double successProbability(String probability) {
		return SUCCESS_PROBABILITY.getOrDefault(probability, 0.3);
	}

	public static boolean isLevelItem(String itemId) {
		return BIG_LEVEL_ITEMS.contains(itemId) || SMALL_LEVEL_ITEMS.contains(itemId);
	}

	private static boolean isHeist(Ad ad) {
		return ad.message() != null && ad.message().startsWith(HEIST_PREFIX);
	}

	private static Optional<ShopItem> findItem(List<ShopItem> items, String id) {
		return items.stream().filter(item -> id.equals(item.id())).findFirst();
	}

	private static Optional<ShopItem> findAffordable(List<ShopItem> items, List<String> ids, int gold,
			int reserve) {
		for (String id : ids) {
			Optional<ShopItem> item = findItem(items, id);
			if (item.isPresent() && gold >= item.get().cost() + reserve) {
				return item;
			}
		}
		return Optional.empty();
	}
}
