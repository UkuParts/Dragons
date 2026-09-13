package com.test.dragons.auto.dto;

import com.test.dragons.game.dto.GameState;

public record AutoMoveResult(boolean finished, StopReason reason, GameState state) {

	public enum StopReason {
		GAME_OVER, TURN_LIMIT, BOARD_DEAD, SKIP_LIMIT
	}
}
