package pl.majchrzw.repoapi;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.majchrzw.repoapi.exception.ExternalApiErrorException;
import pl.majchrzw.repoapi.exception.UserNotFoundException;
import pl.majchrzw.repoapi.model.BranchDto;
import pl.majchrzw.repoapi.model.CommitDto;
import pl.majchrzw.repoapi.model.OwnerDto;
import pl.majchrzw.repoapi.model.RepositoryDto;
import pl.majchrzw.repoapi.service.GithubApiService;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;

@WebFluxTest
public class MainControllerTests {
	
	@MockBean
	private GithubApiService apiService;
	
	@Autowired
	private WebTestClient webTestClient;
	
	@Test
	void testWhenNormalUserIsFound() {
		// given
		Flux<RepositoryDto> repos = Flux.just(
				new RepositoryDto("first repo", new OwnerDto("user"), false, List.of(
						new BranchDto("main", new CommitDto("sha of commit1")),
						new BranchDto("second", new CommitDto("sha of commit2"))
				)),
				new RepositoryDto("second repo", new OwnerDto("user"), false, List.of(
						new BranchDto("main", new CommitDto("sha of commit1"))
				))
		);
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenReturn(repos);
		// when
		var response = webTestClient.get()
				.uri("/api/user")
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		// then
		response.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$[*].name").value(containsInAnyOrder("first repo", "second repo"))
				.jsonPath("$[*].owner.login").value(everyItem(equalTo("user")))
				.jsonPath("$[0].branches[*].name").value(containsInAnyOrder("main", "second"))
				.jsonPath("$[1].branches[*].name").value(containsInAnyOrder("main"));
	}
	
	@Test
	void testWhenNoUserIsFound() {
		// given
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenThrow(new UserNotFoundException("Not found"));
		// when
		var res = webTestClient.get()
				.uri("/api/user")
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		// then
		res.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").exists();
	}
	
	@Test
	void testWhenExternalApiExceptionIsThrown() {
		// given
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenThrow(new ExternalApiErrorException("External api error"));
		// when
		var res = webTestClient.get()
				.uri("/api/user")
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		// then
		res.expectStatus().is5xxServerError()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").exists();
	}
	
	@Test
	void testWhenUserHasNoRepositories() {
		// given
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenReturn(Flux.just());
		// when
		var res = webTestClient.get()
				.uri("/api/user")
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		// then
		res.expectStatus().isOk()
						.expectHeader().contentType(MediaType.APPLICATION_JSON)
						.expectBody().json("[]");
	}
	
	@Test
	void testWhenUserHasRepoWithoutBranches() {
		// given
		Flux<RepositoryDto> repos = Flux.just(
				new RepositoryDto("first repo", new OwnerDto("user"), false, Collections.emptyList()),
				new RepositoryDto("second repo", new OwnerDto("user"), false, List.of(
						new BranchDto("main", new CommitDto("sha of commit1"))
				))
		);
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenReturn(repos);
		// when
		var res = webTestClient.get()
				.uri("/api/user")
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
		// then
		res.expectStatus().isOk()
						.expectHeader().contentType(MediaType.APPLICATION_JSON)
						.expectBody()
						.jsonPath("$[*].name").value(containsInAnyOrder("first repo", "second repo"))
						.jsonPath("$[*].owner.login").value(everyItem(equalTo("user")))
						.jsonPath("$[1].branches[*].name").value(containsInAnyOrder("main"));
	}
}
