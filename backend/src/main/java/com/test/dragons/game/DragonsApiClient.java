package com.test.dragons.game;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;
import com.test.dragons.game.dto.PurchaseResult;
import com.test.dragons.game.dto.Reputation;
import com.test.dragons.game.dto.ShopItem;
import com.test.dragons.game.dto.SolveResult;

/**
 * Thin client around the official Dragons of Mugloar game API.
 */
@Component
public class DragonsApiClient {

	private static final Logger log = LoggerFactory.getLogger(DragonsApiClient.class);

	private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(30);

	private static final ParameterizedTypeReference<List<Ad>> AD_LIST = new ParameterizedTypeReference<>() {
	};
	private static final ParameterizedTypeReference<List<ShopItem>> SHOP_ITEM_LIST = new ParameterizedTypeReference<>() {
	};

	private final RestClient client;
	private final int maxAttempts;
	private final Duration retryBackoff;

	public DragonsApiClient(RestClient dragonsRestClient,
			@Value("${dragons.api.retry.max-attempts:3}") int maxAttempts,
			@Value("${dragons.api.retry.backoff:1s}") Duration retryBackoff) {
		this.client = dragonsRestClient;
		this.maxAttempts = Math.max(1, maxAttempts);
		this.retryBackoff = retryBackoff;
	}

	public GameStart startGame() {
		return withRateLimitRetry(() -> requireBody(client.post()
				.uri("/game/start")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(GameStart.class)));
	}

	public List<Ad> getMessages(String gameId) {
		return withRateLimitRetry(() -> requireBody(client.get()
				.uri("/{gameId}/messages", gameId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(AD_LIST)));
	}

	public SolveResult solveMessage(String gameId, String adId) {
		return withRateLimitRetry(() -> requireBody(client.post()
				.uri("/{gameId}/solve/{adId}", gameId, adId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(SolveResult.class)));
	}

	public List<ShopItem> getShopItems(String gameId) {
		return withRateLimitRetry(() -> requireBody(client.get()
				.uri("/{gameId}/shop", gameId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(SHOP_ITEM_LIST)));
	}

	public PurchaseResult buyItem(String gameId, String itemId) {
		return withRateLimitRetry(() -> requireBody(client.post()
				.uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(PurchaseResult.class)));
	}

	public Reputation investigateReputation(String gameId) {
		return withRateLimitRetry(() -> requireBody(client.post()
				.uri("/{gameId}/investigate/reputation", gameId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(Reputation.class)));
	}

	private <T> T withRateLimitRetry(Supplier<T> request) {
		for (int attempt = 1;; attempt++) {
			try {
				return request.get();
			} catch (RestClientResponseException exception) {
				if (attempt >= maxAttempts
						|| !exception.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
					throw exception;
				}
				Duration delay = retryDelay(exception, attempt);
				log.warn("Dragons of Mugloar API rate limited (attempt {}/{}), retrying in {} ms",
						attempt, maxAttempts, delay.toMillis());
				sleep(delay);
			}
		}
	}

	Duration retryDelay(RestClientResponseException exception, int attempt) {
		Duration delay = retryAfter(exception);
		if (delay == null) {
			delay = retryBackoff.multipliedBy(attempt);
		}
		return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
	}

	static Duration retryAfter(RestClientResponseException exception) {
		HttpHeaders headers = exception.getResponseHeaders();
		String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		try {
			long seconds = Long.parseLong(trimmed);
			return seconds > 0 ? Duration.ofSeconds(seconds) : null;
		} catch (NumberFormatException ignored) {
			return timeUntil(trimmed);
		}
	}

	private static Duration timeUntil(String httpDate) {
		try {
			ZonedDateTime retryAt = ZonedDateTime.parse(httpDate, DateTimeFormatter.RFC_1123_DATE_TIME);
			Duration delay = Duration.between(Instant.now(), retryAt);
			return delay.isNegative() || delay.isZero() ? null : delay;
		} catch (DateTimeParseException ignored) {
			return null;
		}
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException cause) {
			Thread.currentThread().interrupt();
			throw new RestClientException("Interrupted while retrying a rate-limited request.", cause);
		}
	}

	private static <T> T requireBody(T body) {
		if (body == null) {
			throw new RestClientException("The Dragons of Mugloar API returned an empty response body.");
		}
		return body;
	}
}
