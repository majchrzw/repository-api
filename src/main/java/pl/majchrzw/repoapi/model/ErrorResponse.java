package pl.majchrzw.repoapi.model;

public record ErrorResponse(
		int status,
		String message
) {
}
