package ar.edu.um.turnos.appointment.shared.technical;

import ar.edu.um.turnos.appointment.shared.config.CatedraProperties;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** HTTP client for authenticated calls made with this project's technical catedra identity. */
@Component
public class CatedraTechnicalClient {

    private static final String AUTHENTICATE_PATH = "/api/authenticate";
    private static final String BEARER_PREFIX = "Bearer ";

    private final CatedraProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private volatile String cachedToken;

    public CatedraTechnicalClient(CatedraProperties properties,
                                 @Qualifier("catedraRestClient") RestClient catedraRestClient,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = catedraRestClient;
        this.objectMapper = objectMapper;
        this.cachedToken = properties.api().hasConfiguredToken() ? properties.api().jwt() : null;
    }

    /** Performs an authenticated GET and refreshes a rejected login token at most once. */
    public String get(String path) {
        validatePath(path);
        String token = technicalToken();
        try {
            return get(path, token);
        } catch (RejectedTechnicalTokenException rejectedToken) {
            if (properties.api().hasConfiguredToken() || !properties.api().hasLoginCredentials()) {
                throw authenticationFailed();
            }

            String refreshedToken = renewToken(rejectedToken.token());
            try {
                return get(path, refreshedToken);
            } catch (RejectedTechnicalTokenException stillRejected) {
                synchronized (this) {
                    cachedToken = null;
                }
                throw authenticationFailed();
            }
        }
    }

    /** Executes a non-idempotent POST once; network failures are never retried blindly. */
    public String post(String path, Object body) {
        validatePath(path);
        String token = technicalToken();
        try {
            return post(path, token, body);
        } catch (RejectedTechnicalTokenException rejectedToken) {
            if (properties.api().hasConfiguredToken() || !properties.api().hasLoginCredentials()) {
                throw authenticationFailed();
            }
            String refreshedToken = renewToken(rejectedToken.token());
            try {
                return post(path, refreshedToken, body);
            } catch (RejectedTechnicalTokenException stillRejected) {
                synchronized (this) {
                    cachedToken = null;
                }
                throw authenticationFailed();
            }
        } catch (RetryableCatedraCallException ambiguousFailure) {
            throw new CatedraClientException(
                    "CATEDRA_UNAVAILABLE", 503,
                    "The catedra response was unavailable; the operation was not retried.");
        }
    }

    private String technicalToken() {
        String token = cachedToken;
        if (hasText(token)) {
            return token;
        }
        synchronized (this) {
            if (hasText(cachedToken)) {
                return cachedToken;
            }
            if (!properties.api().hasLoginCredentials()) {
                throw new CatedraClientException(
                        "CATEDRA_NOT_CONFIGURED", 503,
                        "The technical account credentials or token are not configured.");
            }
            cachedToken = withRetry(this::requestTechnicalToken);
            return cachedToken;
        }
    }

    private synchronized String renewToken(String rejectedToken) {
        if (hasText(cachedToken) && !cachedToken.equals(rejectedToken)) {
            return cachedToken;
        }
        cachedToken = null;
        cachedToken = withRetry(this::requestTechnicalToken);
        return cachedToken;
    }

    private String requestTechnicalToken() {
        if (!properties.api().hasLoginCredentials()) {
            throw new CatedraClientException(
                    "CATEDRA_NOT_CONFIGURED", 503,
                    "The technical account credentials are not configured.");
        }

        String responseBody = executeWithRetry(() -> {
            try {
                return restClient.post()
                        .uri(absoluteUri(AUTHENTICATE_PATH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AuthenticationRequest(properties.api().login(), properties.api().password(), true))
                        .retrieve()
                        .body(String.class);
            } catch (RestClientResponseException failure) {
                throw translateResponseFailure(failure, true);
            } catch (RestClientException failure) {
                throw translateClientFailure(failure);
            }
        });

        try {
            AuthenticationResponse response = objectMapper.readValue(responseBody, AuthenticationResponse.class);
            if (response == null || !hasText(response.idToken())) {
                throw invalidResponse("The catedra authentication response is missing id_token.");
            }
            return response.idToken();
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw invalidResponse("The catedra authentication response could not be parsed.");
        }
    }

    private String get(String path, String token) {
        return executeWithRetry(() -> {
            try {
                ResponseEntity<String> response = restClient.get()
                        .uri(absoluteUri(path))
                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                        .retrieve()
                        .toEntity(String.class);
                return response.getBody();
            } catch (RestClientResponseException failure) {
                throw translateResponseFailure(failure, false, token);
            } catch (RestClientException failure) {
                throw translateClientFailure(failure);
            }
        });
    }

    private String post(String path, String token, Object body) {
        try {
            return restClient.post()
                    .uri(absoluteUri(path))
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException failure) {
            throw translateResponseFailure(failure, false, token);
        } catch (RestClientException failure) {
            throw translateClientFailure(failure);
        }
    }

    private static RuntimeException translateClientFailure(RestClientException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ResourceAccessException || cause instanceof java.io.IOException) {
                return new RetryableCatedraCallException();
            }
            cause = cause.getCause();
        }
        return new CatedraClientException(
                "CATEDRA_INVALID_RESPONSE", 503,
                "The catedra returned a response that could not be read.");
    }

    private RuntimeException translateResponseFailure(RestClientResponseException failure,
                                                       boolean authenticationCall) {
        int status = failure.getStatusCode().value();
        if (authenticationCall && (status == 401 || status == 403)) {
            return authenticationFailed();
        }
        if (failure.getStatusCode().is5xxServerError()) {
            return new RetryableCatedraCallException();
        }
        String upstreamCode = upstreamCode(failure.getResponseBodyAsString());
        return new CatedraClientException(
                upstreamCode == null ? "CATEDRA_ERROR" : upstreamCode,
                status,
                "The catedra rejected the technical request.");
    }

    private String upstreamCode(String responseBody) {
        if (!hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode code = root == null || !root.isObject() ? null : root.get("code");
            return code != null && code.isTextual() && hasText(code.asText()) ? code.asText() : null;
        } catch (JsonProcessingException malformedBody) {
            return null;
        }
    }

    private RuntimeException translateResponseFailure(RestClientResponseException failure,
                                                       boolean authenticationCall,
                                                       String token) {
        if (failure.getStatusCode().value() == 401 && !authenticationCall) {
            return new RejectedTechnicalTokenException(token);
        }
        return translateResponseFailure(failure, authenticationCall);
    }

    private <T> T withRetry(Supplier<T> action) {
        return executeWithRetry(action);
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        CatedraProperties.Retry retry = properties.retry();
        int maxAttempts = Math.max(1, retry.maxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RetryableCatedraCallException failure) {
                if (attempt == maxAttempts) {
                    throw new CatedraClientException(
                            "CATEDRA_UNAVAILABLE", 503,
                            "The catedra API is not available.");
                }
                pause(retry.backoffFor(attempt));
            }
        }
        throw new CatedraClientException("CATEDRA_UNAVAILABLE", 503, "The catedra API is not available.");
    }

    private static void pause(long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CatedraClientException(
                    "CATEDRA_UNAVAILABLE", 503, "The catedra retry was interrupted.");
        }
    }

    private URI absoluteUri(String path) {
        try {
            return URI.create(properties.api().baseUrl().replaceAll("/+$", "") + path);
        } catch (IllegalArgumentException invalidUrl) {
            throw new CatedraClientException(
                    "CATEDRA_NOT_CONFIGURED", 503, "The catedra API base URL is invalid.");
        }
    }

    private static void validatePath(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//")) {
            throw new IllegalArgumentException("A catedra API path must be an absolute local path.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static CatedraClientException authenticationFailed() {
        return new CatedraClientException(
                "CATEDRA_AUTH_FAILED", 503,
                "The catedra rejected the technical credentials of this project.");
    }

    private static CatedraClientException invalidResponse(String message) {
        return new CatedraClientException("CATEDRA_INVALID_RESPONSE", 503, message);
    }

    private record AuthenticationRequest(String username, String password, boolean rememberMe) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthenticationResponse(@JsonProperty("id_token") String idToken) {
    }

    private static final class RetryableCatedraCallException extends RuntimeException {
    }

    private static final class RejectedTechnicalTokenException extends RuntimeException {

        private final String token;

        private RejectedTechnicalTokenException(String token) {
            this.token = token;
        }

        private String token() {
            return token;
        }
    }
}
