package pl.majchrzw.repoapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.majchrzw.repoapi.model.RepositoryDto;
import pl.majchrzw.repoapi.service.GithubApiService;

import java.util.List;

@RestController
public class MainController {
	
	private final GithubApiService githubApiService;
	
	public MainController(GithubApiService githubApiService) {
		this.githubApiService = githubApiService;
	}
	
	@GetMapping(value = "/api/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get list of user repositories that are not forks and each repository list of branches")
	public List<RepositoryDto> getRepository(@PathVariable String username) {
		return githubApiService.getRepositoriesAndBranchesOfUser(username);
	}
}
