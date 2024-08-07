package pl.majchrzw.repoapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.majchrzw.repoapi.exception.ExternalApiErrorException;
import pl.majchrzw.repoapi.exception.UserNotFoundException;
import pl.majchrzw.repoapi.model.BranchDto;
import pl.majchrzw.repoapi.model.RepositoryDto;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.function.Predicate;

@Service
public class GithubApiService {
	
	private final WebClient webClient;
	private final Logger log = LoggerFactory.getLogger(GithubApiService.class);
	
	public GithubApiService(WebClient webClient) {
		this.webClient = webClient;
	}
	
	public Flux<RepositoryDto> getRepositoriesAndBranchesOfUser(String username) {
		log.info("GET request for repositories and branches of user {}", username);
		// log.info("Successfully got repositories and branches of user {}, repositories count {}", username, repositories.size());
		return getRepositoriesOfUser(username)
				.filter(Predicate.not(RepositoryDto::fork))
				.publishOn(Schedulers.boundedElastic())
				.map(repo -> {
					Flux<BranchDto> branches = getBranchesForRepository(repo.owner().login(), repo.name());
					
					return new RepositoryDto(repo.name(), repo.owner(), repo.fork(), branches.collectList().block());
				}).doOnComplete(() -> log.info("Data fetch for user {} successful", username));
	}
	
	private Flux<RepositoryDto> getRepositoriesOfUser(String username) {
		
		return webClient.get()
				.uri("/users/{owner}/repos", username)
				.retrieve()
				.onStatus(status -> status.value() == 404, ((response) -> {
					log.info("No user with username '{}' found on github", username);
					throw new UserNotFoundException("No user with username '" + username + "', has been found!");
				}))
				.onStatus(HttpStatusCode::is4xxClientError, ((response) -> {
					log.warn("Request for user '{}' repositories has returned error {}: {}", username, response.statusCode(), "TEST");
					throw new ExternalApiErrorException("Cannot realize request due to github api error, try later.");
				}))
				.bodyToFlux(RepositoryDto.class);
	}
	
	private Flux<BranchDto> getBranchesForRepository(String username, String repository) {
		
		return webClient.get()
				.uri("/repos/{owner}/{repo}/branches", username, repository)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, (response) -> {
					log.warn("Request for repository '{}' branches has returned error {}: {}", repository, response.statusCode(), "TEST");
					throw new ExternalApiErrorException("Cannot realize request due to github api error, try later.");
				})
				.bodyToFlux(BranchDto.class);
	}
}
