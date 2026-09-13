package com.test.dragons.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.test.dragons.game.dto.GameApiError;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class GameApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GameApiExceptionHandler.class);

	private static final String GENERIC_MESSAGE = "The Dragons of Mugloar API rejected the request.";
	private static final List<String> MESSAGE_FIELDS = List.of("error", "status");

	private final ObjectMapper objectMapper;

	public GameApiExceptionHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<GameApiError> handleUpstreamError(RestClientResponseException exception) {
		HttpStatus status = clientStatus(exception.getStatusCode().value());
		log.warn("Dragons of Mugloar API returned status {}", exception.getStatusCode().value());
		ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
		String retryAfter = retryAfter(exception);
		if (retryAfter != null) {
			response.header(HttpHeaders.RETRY_AFTER, retryAfter);
		}
		return response.body(new GameApiError(upstreamMessage(exception)));
	}

	@ExceptionHandler(RestClientException.class)
	public ResponseEntity<GameApiError> handleUpstreamFailure(RestClientException exception) {
		log.error("Dragons of Mugloar API request failed", exception);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(new GameApiError("The Dragons of Mugloar API is unavailable."));
	}

	private static HttpStatus clientStatus(int upstreamStatus) {
		HttpStatus status = HttpStatus.resolve(upstreamStatus);
		return status != null && status.is4xxClientError() ? status : HttpStatus.BAD_GATEWAY;
	}

	private static String retryAfter(RestClientResponseException exception) {
		HttpHeaders headers = exception.getResponseHeaders();
		return headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
	}

	private String upstreamMessage(RestClientResponseException exception) {
		String body = exception.getResponseBodyAsString();
		if (body.isBlank()) {
			return GENERIC_MESSAGE;
		}
		try {
			JsonNode payload = objectMapper.readTree(body);
			for (String field : MESSAGE_FIELDS) {
				JsonNode value = payload.get(field);
				if (value != null && value.isValueNode() && !value.asText().isBlank()) {
					return value.asText();
				}
			}
		} catch (JacksonException ignored) {
			// not a JSON payload, fall back to the generic message
		}
		return GENERIC_MESSAGE;
	}
}
