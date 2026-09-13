package com.test.dragons.auto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.test.dragons.auto.AutoStrategy.Action;
import com.test.dragons.auto.AutoStrategy.Buy;
import com.test.dragons.auto.AutoStrategy.Skip;
import com.test.dragons.auto.AutoStrategy.Solve;
import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.ShopItem;

class AutoStrategyTest {

	private static final List<ShopItem> SHOP = List.of(
			new ShopItem("hpot", "Healing potion", 50),
			new ShopItem("cs", "Cheap levels", 100),
			new ShopItem("ch", "Big levels", 300));

	private static Ad ad(String id, String probability, int reward) {
		return new Ad(id, "Fix a wagon", reward, 7, false, probability);
	}

	@Test
	void recognisesHeistsAndLevelItems() {
		assertThat(AutoStrategy.isLevelItem("ch")).isTrue();
		assertThat(AutoStrategy.isLevelItem("cs")).isTrue();
		assertThat(AutoStrategy.isLevelItem("hpot")).isFalse();
	}

	@Test
	void ignoresHeistsAndImpossibleTasksWhenPickingATask() {
		List<Ad> ads = List.of(
				new Ad("heist", "Steal super awesome diamond from somebody.", 999, 7, false, "Sure thing"),
				ad("impossible", "Impossible", 999),
				ad("plain", "Piece of cake", 10));

		assertThat(AutoStrategy.pickTask(ads, 10).adId()).isEqualTo("plain");
	}

	@Test
	void ignoresSuicideMissionsBeforeTurnThirty() {
		List<Ad> ads = List.of(ad("suicide", "Suicide mission", 500), ad("safe", "Sure thing", 10));

		assertThat(AutoStrategy.pickTask(ads, 10).adId()).isEqualTo("safe");
		assertThat(AutoStrategy.pickTask(ads, 30).adId()).isEqualTo("suicide");
	}

	@Test
	void picksTheHighestProbabilityWeightedReward() {
		List<Ad> ads = List.of(ad("rich", "Quite likely", 100), ad("safe", "Piece of cake", 50));

		assertThat(AutoStrategy.pickTask(ads, 10).adId()).isEqualTo("rich");
	}

	@Test
	void healsAtTheLastLifeBeforeAnythingElse() {
		Action action = AutoStrategy.decide(1, 450, 0, 0, List.of(ad("ad-1", "Piece of cake", 10)), SHOP);

		assertThat(action).isEqualTo(new Buy("hpot"));
	}

	@Test
	void buysTheBigLevelItemWhenItCanAffordTheReserve() {
		assertThat(AutoStrategy.decide(4, 450, 0, 0, List.of(), SHOP)).isEqualTo(new Buy("ch"));
	}

	@Test
	void fallsBackToTheSmallLevelItem() {
		assertThat(AutoStrategy.decide(4, 300, 0, 0, List.of(), SHOP)).isEqualTo(new Buy("cs"));
	}

	@Test
	void doesNotBuyLevelsBelowTheReserve() {
		assertThat(AutoStrategy.decide(4, 249, 0, 0, List.of(ad("ad-1", "Piece of cake", 10)), SHOP))
				.isEqualTo(new Solve("ad-1"));
	}

	@Test
	void stopsBuyingLevelsOnceThePurchaseStreakIsUsedUp() {
		assertThat(AutoStrategy.decide(4, 450, 0, 8, List.of(ad("ad-1", "Piece of cake", 10)), SHOP))
				.isEqualTo(new Solve("ad-1"));
	}

	@Test
	void stopsBuyingLevelsAfterTheLevelingTurnLimit() {
		assertThat(AutoStrategy.decide(4, 450, 115, 0, List.of(ad("ad-1", "Piece of cake", 10)), SHOP))
				.isEqualTo(new Solve("ad-1"));
	}

	@Test
	void skipsWhenThereIsNothingLeftToSolve() {
		assertThat(AutoStrategy.decide(4, 0, 0, 0, List.of(), SHOP)).isEqualTo(new Skip());
		assertThat(AutoStrategy.decide(4, 0, 0, 0,
				List.of(new Ad("heist", "Steal super awesome diamond from somebody.", 999, 7, false,
						"Sure thing")),
				SHOP)).isEqualTo(new Skip());
	}

	@Test
	void leavesTerminationToThePlayer() {
		assertThat(AutoStrategy.decide(4, 450, 200, 0, List.of(ad("ad-1", "Piece of cake", 10)), SHOP))
				.isEqualTo(new Solve("ad-1"));
	}
}
