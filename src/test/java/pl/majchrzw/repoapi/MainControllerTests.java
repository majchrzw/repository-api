package pl.majchrzw.repoapi;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pl.majchrzw.repoapi.exception.ExternalApiErrorException;
import pl.majchrzw.repoapi.exception.UserNotFoundException;
import pl.majchrzw.repoapi.model.BranchDto;
import pl.majchrzw.repoapi.model.CommitDto;
import pl.majchrzw.repoapi.model.OwnerDto;
import pl.majchrzw.repoapi.model.RepositoryDto;
import pl.majchrzw.repoapi.service.GithubApiService;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest
public class MainControllerTests {
	
	@MockBean
	private GithubApiService apiService;
	
	@Autowired
	private MockMvc mockMvc;
	
	@Test
	void testWhenNormalUserIsFound() throws Exception {
		// given
		List<RepositoryDto> repos = List.of(
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
		ResultActions res = mockMvc.perform(get("/api/user"));
		// then
		res.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[*].name", containsInAnyOrder("first repo", "second repo")))
				.andExpect(jsonPath("$[*].owner.login", everyItem(equalTo("user"))))
				.andExpect(jsonPath("$[0].branches[*].name", containsInAnyOrder("main", "second")))
				.andExpect(jsonPath("$[1].branches[*].name", containsInAnyOrder("main")));
	}
	
	@Test
	void testWhenNoUserIsFound() throws Exception {
		// given
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenThrow(new UserNotFoundException("Not found"));
		// when
		ResultActions res = mockMvc.perform(get("/api/user"));
		// then
		res.andExpect(status().is(404))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value("404"))
				.andExpect(jsonPath("$.message").exists());
	}
	
	@Test
	void testWhenExternalApiExceptionIsThrown() throws Exception {
		// given
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenThrow(new ExternalApiErrorException("External api error"));
		// when
		ResultActions res = mockMvc.perform(get("/api/user"));
		// then
		res.andExpect(status().is(500))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value("500"))
				.andExpect(jsonPath("$.message").exists());
	}
	
	@Test
	void testWhenUserHasNoRepositories() throws Exception {
		// given
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenReturn(Collections.emptyList());
		// when
		ResultActions res = mockMvc.perform(get("/api/user"));
		// then
		res.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string("[]"));
	}
	
	@Test
	void testWhenUserHasRepoWithoutBranches() throws Exception {
		// given
		List<RepositoryDto> repos = List.of(
				new RepositoryDto("first repo", new OwnerDto("user"), false, Collections.emptyList()),
				new RepositoryDto("second repo", new OwnerDto("user"), false, List.of(
						new BranchDto("main", new CommitDto("sha of commit1"))
				))
		);
		Mockito.when(apiService.getRepositoriesAndBranchesOfUser("user")).thenReturn(repos);
		// when
		ResultActions res = mockMvc.perform(get("/api/user"));
		// then
		res.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[*].name", containsInAnyOrder("first repo", "second repo")))
				.andExpect(jsonPath("$[*].owner.login", everyItem(equalTo("user"))))
				.andExpect(jsonPath("$[1].branches[*].name", containsInAnyOrder("main")));
	}
}
