package ar.edu.um.turnos.appointment.catalog.infrastructure.rest;

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

    public CatalogRestClientSupport(RestClient catalogRestClient) {
        this.restClient = catalogRestClient;
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

    private static CatalogClientException translateResponseFailure(RestClientResponseException failure) {
        int status = failure.getStatusCode().value();
        if (status == 404) {
            return new CatalogClientException(
                    "PROFESSIONAL_NOT_FOUND", 404,
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
                "CATALOG_ERROR", status,
                "The catalog rejected the internal request.");
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
