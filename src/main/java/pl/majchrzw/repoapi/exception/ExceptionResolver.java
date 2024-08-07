package pl.majchrzw.repoapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import pl.majchrzw.repoapi.model.ErrorResponse;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class ExceptionResolver {
	
	@ResponseBody
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(UserNotFoundException.class)
	public Mono<ErrorResponse> handleUserNotFoundException(UserNotFoundException exception) {
		return Mono.just(new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
	}
	
	@ResponseBody
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(ExternalApiErrorException.class)
	public Mono<ErrorResponse> handleExternalApiErrorException(ExternalApiErrorException exception) {
		return Mono.just(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage()));
	}
}
