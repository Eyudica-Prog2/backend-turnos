package ar.edu.um.turnos.appointment.shared.error;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CatalogClientException.class)
    ResponseEntity<ProblemDetail> handleCatalogFailure(CatalogClientException failure) {
        return problem(failure.getCode(), failure.getHttpStatus(), failure.getMessage());
    }

    @ExceptionHandler(CatedraClientException.class)
    ResponseEntity<ProblemDetail> handleCatedraFailure(CatedraClientException failure) {
        return problem(failure.getCode(), failure.getHttpStatus(), failure.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleInvalidRequestParameter(MethodArgumentTypeMismatchException failure) {
        return problem("VALIDATION_ERROR", 400, "A request parameter has an invalid format.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleInvalidArgument(IllegalArgumentException failure) {
        return problem("VALIDATION_ERROR", 400, failure.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(String code, int status, String detail) {
        HttpStatusCode httpStatus = HttpStatusCode.valueOf(status);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        problem.setProperty("code", code);
        return ResponseEntity.status(httpStatus)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
