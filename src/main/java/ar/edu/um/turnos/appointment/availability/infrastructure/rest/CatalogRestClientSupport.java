package ar.edu.um.turnos.appointment.availability.infrastructure.rest;

import ar.edu.um.turnos.appointment.shared.error.CatalogClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CatalogRestClientSupport {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CatalogRestClientSupport(@Qualifier("catalogRestClient") RestClient catalogRestClient,
                                    ObjectMapper objectMapper) {
        this.restClient = catalogRestClient;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String path, String userToken, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + userToken)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException failure) {
            throw translateResponseFailure(failure);
        } catch (ResourceAccessException failure) {
            throw unavailable();
        } catch (RestClientException failure) {
            throw translateClientFailure(failure);
        }
    }

    public <T> T get(String path, String userToken, ParameterizedTypeReference<T> responseType) {
        try {
            return restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + userToken)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException failure) {
            throw translateResponseFailure(failure);
        } catch (ResourceAccessException failure) {
            throw unavailable();
        } catch (RestClientException failure) {
            throw translateClientFailure(failure);
        }
    }

    private static CatalogClientException translateClientFailure(RestClientException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ResourceAccessException || cause instanceof java.io.IOException) {
                return unavailable();
            }
            cause = cause.getCause();
        }
        return invalidResponse();
    }

    private CatalogClientException translateResponseFailure(RestClientResponseException failure) {
        int status = failure.getStatusCode().value();
        String upstreamCode = upstreamCode(failure.getResponseBodyAsString());
        if (status == 404) {
            return new CatalogClientException(
                    upstreamCode == null ? "PROFESSIONAL_NOT_FOUND" : upstreamCode,
                    404,
                    "The professional was not found in the catalog.");
        }
        if (status == 401) {
            return new CatalogClientException(
                    "CATALOG_AUTH_FAILED", 401,
                    "The catalog rejected the propagated user token.");
        }
        if (status == 403) {
            return new CatalogClientException(
                    "CATALOG_ACCESS_DENIED", 403,
                    "The user is not authorized to read the catalog data.");
        }
        if (failure.getStatusCode().is5xxServerError()) {
            return unavailable();
        }
        return new CatalogClientException(
                upstreamCode == null ? "CATALOG_ERROR" : upstreamCode,
                status,
                "The catalog rejected the internal request.");
    }

    private String upstreamCode(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode code = root == null || !root.isObject() ? null : root.get("code");
            return code != null && code.isTextual() && !code.asText().isBlank() ? code.asText() : null;
        } catch (JsonProcessingException malformedBody) {
            return null;
        }
    }

    public static CatalogClientException unavailable() {
        return new CatalogClientException(
                "CATALOG_UNAVAILABLE", 503,
                "The catalog service is not available.");
    }

    public static CatalogClientException invalidResponse() {
        return new CatalogClientException(
                "CATALOG_INVALID_RESPONSE", 502,
                "The catalog returned a response that does not match its internal contract.");
    }
}
