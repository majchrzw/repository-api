package pl.majchrzw.repoapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pl.majchrzw.repoapi.exception.ExternalApiErrorException;
import pl.majchrzw.repoapi.exception.UserNotFoundException;
import pl.majchrzw.repoapi.model.BranchDto;
import pl.majchrzw.repoapi.model.RepositoryDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

@Service
public class GithubApiService {
	
	private final RestClient restClient;
	private final Logger log = LoggerFactory.getLogger(GithubApiService.class);
	
	public GithubApiService(RestClient restClient) {
		this.restClient = restClient;
	}
	
	public List<RepositoryDto> getRepositoriesAndBranchesOfUser(String username) {
		log.info("GET request for repositories and branches of user {}", username);
		var repositories = getRepositoriesOfUser(username).stream()
				.filter(Predicate.not(RepositoryDto::fork))
				.map(repo -> {
					List<BranchDto> branches = getBranchesForRepository(repo.owner().login(), repo.name()).stream()
							.toList();
					
					return new RepositoryDto(repo.name(), repo.owner(), repo.fork(), branches);
				}).toList();
		log.info("Successfully got repositories and branches of user {}, repositories count {}", username, repositories.size());
		
		return repositories;
	}
	
	private List<RepositoryDto> getRepositoriesOfUser(String username) {
		var repositories = restClient.get()
				.uri("/users/{owner}/repos", username)
				.retrieve()
				.onStatus(status -> status.value() == 404, ((request, response) -> {
					log.info("No user with username '{}' found on github", username);
					throw new UserNotFoundException("No user with username '" + username + "', has been found!");
				}))
				.onStatus(HttpStatusCode::is4xxClientError, ((request, response) -> {
					log.warn("Request for user '{}' repositories has returned error {}: {}", username, response.getStatusCode(), new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
					throw new ExternalApiErrorException("Cannot realize request due to github api error, try later.");
				}))
				.body(new ParameterizedTypeReference<List<RepositoryDto>>() {
				});
		
		if (repositories == null) {
			log.warn("Failed to retrieve repositories for user '{}'", username);
			throw new ExternalApiErrorException("Failed to retrieve repositories for user '" + username + "'");
		}
		
		return repositories;
	}
	
	private List<BranchDto> getBranchesForRepository(String username, String repository) {
		var branches = restClient.get()
				.uri("/repos/{owner}/{repo}/branches", username, repository)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, ((request, response) -> {
					log.warn("Request for repository '{}' branches has returned error {}: {}", repository, response.getStatusCode(), new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
					throw new ExternalApiErrorException("Cannot realize request due to github api error, try later.");
				}))
				.body(new ParameterizedTypeReference<List<BranchDto>>() {
				});
		
		if (branches == null) {
			log.warn("Failed to retrieve branch info for repository '{}'", repository);
			throw new ExternalApiErrorException("Failed to retrieve branch info for repository '" + repository + "'");
		}
		
		return branches;
	}
}
