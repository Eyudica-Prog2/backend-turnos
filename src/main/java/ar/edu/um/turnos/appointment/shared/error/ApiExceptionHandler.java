package ar.edu.um.turnos.appointment.shared.error;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(ReservationException.class)
    ResponseEntity<ProblemDetail> handleReservationFailure(ReservationException failure,
                                                            HttpServletRequest request) {
        return problem(failure.getCode(), failure.getHttpStatus(), failure.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidationFailure(MethodArgumentNotValidException failure,
                                                           HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(400), "The request contains invalid fields.");
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("path", request.getRequestURI());
        List<Map<String, String>> fieldErrors = failure.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("objectName", error.getObjectName(),
                        "field", error.getField(), "message", error.getDefaultMessage() == null
                                ? "invalid value" : error.getDefaultMessage()))
                .toList();
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException failure,
                                                        HttpServletRequest request) {
        return problem("VALIDATION_ERROR", 400, "The request body is invalid.", request);
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
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setProperty("code", code);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static ResponseEntity<ProblemDetail> problem(String code, int status, String detail,
                                                          HttpServletRequest request) {
        HttpStatusCode httpStatus = HttpStatusCode.valueOf(status);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        problem.setProperty("code", code);
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(httpStatus)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
