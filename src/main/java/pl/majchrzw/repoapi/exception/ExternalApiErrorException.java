package pl.majchrzw.repoapi.exception;

public class ExternalApiErrorException extends RuntimeException {
	String message;
	
	public ExternalApiErrorException(String message) {
		super(message);
		this.message = message;
	}
}
