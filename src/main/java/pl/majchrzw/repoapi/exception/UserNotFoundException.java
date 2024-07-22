package pl.majchrzw.repoapi.exception;

public class UserNotFoundException extends RuntimeException {
	String message;
	
	public UserNotFoundException(String message) {
		super(message);
		this.message = message;
	}
}
