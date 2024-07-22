package pl.majchrzw.repoapi;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.majchrzw.repoapi.exception.ExternalApiErrorException;
import pl.majchrzw.repoapi.exception.UserNotFoundException;
import pl.majchrzw.repoapi.model.RepositoryDto;
import pl.majchrzw.repoapi.service.GithubApiService;

import java.io.IOException;
import java.util.List;

@SpringBootTest()
public class GithubApiServiceTests {
	
	static MockWebServer webServer;
	private final String emptyCollection = "[]";
	private final String oneRepo =
			"""
							[
							  {
							    "id": 1296269,
							    "name": "Hello-World",
							    "owner": {
							      "login": "octocat"
							    },
							    "fork": false
							  }
							]
					""";
	private final String oneBranch =
			"""
					  [
					      {
					        "name": "master",
					        "commit": {
					          "sha": "c5b97d5ae6c19d5c5df71a34c7fbeeda2479ccbc"
					        }
					      }
					    ]
					""";
	@Autowired
	private GithubApiService githubApiService;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry r) {
		r.add("gh-api-url", () -> "http://localhost:" + webServer.getPort());
	}

	@BeforeAll
	static void beforeAll() throws IOException {
		webServer = new MockWebServer();
		webServer.start();
	}

	@AfterAll
	static void afterAll() throws IOException {
		webServer.shutdown();
	}
	
	@Test
	void emptyRepoResponseTest() {
		// given
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(emptyCollection));
		int initialCount = webServer.getRequestCount();
		// when
		List<RepositoryDto> res = githubApiService.getRepositoriesAndBranchesOfUser("wojmaj22");
		// then
		Assertions.assertEquals(0, res.size());
		Assertions.assertEquals(1, webServer.getRequestCount() - initialCount);
	}
	
	@Test
	void oneRepoResponseTest() {
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneRepo));
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneBranch));
		int initialCount = webServer.getRequestCount();
		// when
		List<RepositoryDto> res = githubApiService.getRepositoriesAndBranchesOfUser("octocat");
		// then
		Assertions.assertEquals(1, res.size());
		Assertions.assertEquals("Hello-World", res.getFirst().name());
		Assertions.assertEquals("octocat", res.getFirst().owner().login());
		Assertions.assertFalse(res.getFirst().fork());
		Assertions.assertEquals(1, res.getFirst().branches().size());
		Assertions.assertEquals("master", res.getFirst().branches().getFirst().name());
		Assertions.assertEquals("c5b97d5ae6c19d5c5df71a34c7fbeeda2479ccbc", res.getFirst().branches().getFirst().lastCommit().sha());
		Assertions.assertEquals(2, webServer.getRequestCount() - initialCount);
	}
	
	@Test
	void normalAndForkRepoResponseTest() {
		// given
		String oneRepoAndOneForkRepo = """
				[
				  {
						    "id": 1296269,
						    "name": "Hello-World",
						    "owner": {
						      "login": "octocat"
						    },
						    "fork": false
				  },
				  {
						    "id": 1296270,
						    "name": "Hello-World2",
						    "owner": {
						      "login": "octocat"
						    },
						    "fork": true
				  }
				]
				""";
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneRepoAndOneForkRepo));
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneBranch));
		int initialCount = webServer.getRequestCount();
		// when
		List<RepositoryDto> res = githubApiService.getRepositoriesAndBranchesOfUser("octocat");
		// then
		Assertions.assertEquals(1, res.size());
		Assertions.assertFalse(res.getFirst().fork());
		Assertions.assertEquals(1, res.getFirst().branches().size());
		Assertions.assertEquals(2, webServer.getRequestCount() - initialCount);
	}
	
	@Test
	void emptyBranchResponseTest() {
		// given
		String twoRepos = """
					[
				  {
						    "id": 1296269,
						    "name": "Hello-World",
						    "owner": {
						      "login": "octocat"
						    },
						    "fork": false
				  },
				  {
						    "id": 1296270,
						    "name": "Hello-World2",
						    "owner": {
						      "login": "octocat"
						    },
						    "fork": false
				  }
				]
				""";
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(
						twoRepos
				));
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneBranch));
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(emptyCollection));
		int initialCount = webServer.getRequestCount();
		// when
		List<RepositoryDto> res = githubApiService.getRepositoriesAndBranchesOfUser("octocat");
		// then
		Assertions.assertEquals(2, res.size());
		Assertions.assertEquals(1, res.getFirst().branches().size());
		Assertions.assertEquals(0, res.getLast().branches().size());
		Assertions.assertEquals(3, webServer.getRequestCount() - initialCount);
	}
	
	@Test
	void repoWithManyBranchesResponseTest() {
		//given
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneRepo));
		String twoBranches = """
				  [
				      {
				        "name": "master",
				        "commit": {
				          "sha": "c5b97d5ae6c19d5c5df71a34c7fbeeda2479ccbc"
				        }
				      },
				      {
				        "name": "master",
				        "commit": {
				          "sha": "c5b97d5ae6c19d5c5df71a34c7fbeeda2479ccbc"
				        }
				      }
				    ]
				""";
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(twoBranches));
		int initialCount = webServer.getRequestCount();
		// when
		List<RepositoryDto> res = githubApiService.getRepositoriesAndBranchesOfUser("octocat");
		// then
		Assertions.assertEquals(1, res.size());
		Assertions.assertEquals(2, res.getFirst().branches().size());
		Assertions.assertEquals(2, webServer.getRequestCount() - initialCount);
	}
	
	@Test
	void noUserFoundResponseTest() {
		// given
		webServer.enqueue(new MockResponse()
				.setResponseCode(404)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
		// then
		Assertions.assertThrows(UserNotFoundException.class, () -> githubApiService.getRepositoriesAndBranchesOfUser("not_existing"));
	}
	
	@Test
	void rateLimitExceededResponseTest() {
		// given
		webServer.enqueue(new MockResponse()
				.setResponseCode(403)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
		// then
		Assertions.assertThrows(ExternalApiErrorException.class, () -> githubApiService.getRepositoriesAndBranchesOfUser("some_user"));
	}
	
	@Test
	void noRepoResponseTest() {
		// given
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(""));
		// then
		Assertions.assertThrows(ExternalApiErrorException.class, () -> githubApiService.getRepositoriesAndBranchesOfUser("random"));
	}
	
	@Test
	void noBranchResponseTest() {
		// given
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneRepo));
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(""));
		// then
		Assertions.assertThrows(ExternalApiErrorException.class, () -> githubApiService.getRepositoriesAndBranchesOfUser("random"));
	}
	
	@Test
	void rateLimitedOnBranchResponseTest() {
		// given
		webServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(oneRepo));
		webServer.enqueue(new MockResponse()
				.setResponseCode(403)
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody("[]"));
		// then
		Assertions.assertThrows(ExternalApiErrorException.class, () -> githubApiService.getRepositoriesAndBranchesOfUser("random"));
	}
}
