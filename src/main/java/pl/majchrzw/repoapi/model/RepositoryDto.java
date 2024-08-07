package pl.majchrzw.repoapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;

public record RepositoryDto(
		String name,
		OwnerDto owner,
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		boolean fork,
		Flux<BranchDto> branches
) {
}
