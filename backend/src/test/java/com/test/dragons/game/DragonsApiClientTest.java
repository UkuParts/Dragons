package com.test.dragons.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.test.dragons.game.dto.Ad;
import com.test.dragons.game.dto.GameStart;

class DragonsApiClientTest {

	private final RestClient.Builder builder = RestClient.builder().baseUrl("https://dragons.test");
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final DragonsApiClient client = new DragonsApiClient(builder.build(), 3, Duration.ZERO);

	@Test
	void retriesRateLimitedRequests() {
		server.expect(requestTo("https://dragons.test/game/start"))
				.andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		server.expect(requestTo("https://dragons.test/game/start"))
				.andRespond(withSuccess("""
						{"gameId":"game-1","lives":3,"gold":0,"level":0,"score":0,"highScore":0,"turn":0}
						""", MediaType.APPLICATION_JSON));

		GameStart game = client.startGame();

		assertThat(game.gameId()).isEqualTo("game-1");
		server.verify();
	}

	@Test
	void stopsRetryingWhenAttemptsRunOut() {
		server.expect(requestTo("https://dragons.test/game/start"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		server.expect(requestTo("https://dragons.test/game/start"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		server.expect(requestTo("https://dragons.test/game/start"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThatThrownBy(client::startGame).isInstanceOf(RestClientResponseException.class);

		server.verify();
	}

	@Test
	void doesNotRetryOtherStatuses() {
		server.expect(requestTo("https://dragons.test/game/start"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(client::startGame).isInstanceOf(RestClientResponseException.class);

		server.verify();
	}

	@Test
	void usesRetryAfterSeconds() {
		HttpClientErrorException exception = rateLimited("5");

		assertThat(client.retryDelay(exception, 1)).isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	void capsRetryAfterAtThirtySeconds() {
		HttpClientErrorException exception = rateLimited("3600");

		assertThat(client.retryDelay(exception, 1)).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void parsesHttpDateRetryAfter() {
		String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME
				.format(ZonedDateTime.now(ZoneOffset.UTC).plusHours(1));

		assertThat(client.retryDelay(rateLimited(httpDate), 1)).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void fallsBackToBackoffForPastHttpDates() {
		DragonsApiClient backoffClient = new DragonsApiClient(builder.build(), 3, Duration.ofSeconds(2));

		assertThat(backoffClient.retryDelay(rateLimited("Wed, 21 Oct 2015 07:28:00 GMT"), 2))
				.isEqualTo(Duration.ofSeconds(4));
	}

	@Test
	void fallsBackToBackoffForInvalidRetryAfterValues() {
		DragonsApiClient backoffClient = new DragonsApiClient(builder.build(), 3, Duration.ofSeconds(2));

		assertThat(backoffClient.retryDelay(rateLimited("soon"), 2)).isEqualTo(Duration.ofSeconds(4));
	}

	@Test
	void readsNumericEncryptedFlagAsBoolean() {
		server.expect(requestTo("https://dragons.test/game-1/messages"))
				.andRespond(withSuccess("""
						[{"adId":"YWRJZA==","message":"bWVzc2FnZQ==","reward":10,"expiresIn":3,
						  "encrypted":1,"probability":"U3VyZSB0aGluZw=="}]
						""", MediaType.APPLICATION_JSON));

		List<Ad> messages = client.getMessages("game-1");

		assertThat(messages).hasSize(1);
		assertThat(messages.getFirst().encrypted()).isTrue();
		assertThat(messages.getFirst().probability()).isEqualTo("U3VyZSB0aGluZw==");
		server.verify();
	}

	private static HttpClientErrorException rateLimited(String retryAfter) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
		return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
				headers, new byte[0], null);
	}
}
