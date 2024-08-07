package pl.majchrzw.repoapi.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RestClientConfiguration {
	
	private final Logger log = LoggerFactory.getLogger(RestClientConfiguration.class);
	@Value("${gh-access-token}")
	private String apiKey;
	
	@Value("${gh-api-url}")
	private String url;
	
	@Bean
	public WebClient webClient() {
		if (url.isBlank()) {
			log.error("No api url provided!");
		}
		return WebClient.builder()
				.baseUrl(url)
				.defaultHeader("X-GitHub-Api-Version", "2022-11-28")
				.defaultHeader("Accept", "application/vnd.github+json")
				.defaultHeader("Authorization", checkForAuthHeader())
				.build();
	}
	
	private String checkForAuthHeader() {
		if (!apiKey.isBlank()) {
			log.info("Initialized webClient with github PAT.");
			return "Bearer ".concat(apiKey);
		} else {
			log.info("No PAT detected, running without it. Requests may be limited.");
			return "";
		}
	}
}
