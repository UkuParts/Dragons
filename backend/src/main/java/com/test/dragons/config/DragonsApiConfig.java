package com.test.dragons.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DragonsApiConfig {

	@Bean
	public RestClient dragonsRestClient(
			@Value("${dragons.api.base-url}") String baseUrl,
			@Value("${dragons.api.connect-timeout:5s}") Duration connectTimeout,
			@Value("${dragons.api.read-timeout:10s}") Duration readTimeout) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);
		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
	}
}
