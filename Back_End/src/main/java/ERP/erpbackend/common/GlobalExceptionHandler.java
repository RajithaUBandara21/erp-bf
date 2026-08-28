package ERP.erpbackend.common;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Extends ResponseEntityExceptionHandler so Spring MVC's own client-error exceptions (malformed JSON
// body, unsupported method, missing parameter) are mapped to 4xx instead of falling through to the
// Exception.class handler as a 500 + error log. Its handlers default to a ProblemDetail body, so
// handleExceptionInternal is overridden to keep the project's { message, errors } shape.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.<Object>body(new ErrorResponse("Validation failed", fieldErrors));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
		log.warn("Data integrity conflict", ex);
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("The request conflicts with existing data. Please try again.", Map.of()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode())
				.body(new ErrorResponse(ex.getReason(), Map.of()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("An unexpected error occurred", Map.of()));
	}

	// The inherited handlers hand us a ProblemDetail (or null) body; swap it for the project's shape.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		Object responseBody = body;
		if (body == null || body instanceof ProblemDetail) {
			String message = statusCode.is4xxClientError()
					? "The request could not be processed. Check the request body, method, and parameters."
					: "An unexpected error occurred";
			responseBody = new ErrorResponse(message, Map.of());
		}
		return super.handleExceptionInternal(ex, responseBody, headers, statusCode, request);
	}

	public record ErrorResponse(String message, Map<String, String> errors) {
	}

}
