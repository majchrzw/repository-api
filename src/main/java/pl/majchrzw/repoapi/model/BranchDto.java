package pl.majchrzw.repoapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BranchDto(
		String name,
		@JsonAlias({"commit"})
		CommitDto lastCommit
) {
}
