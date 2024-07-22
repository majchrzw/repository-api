package pl.majchrzw.repoapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RepositoryDto(
		String name,
		OwnerDto owner,
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		boolean fork,
		List<BranchDto> branches
) {
}
