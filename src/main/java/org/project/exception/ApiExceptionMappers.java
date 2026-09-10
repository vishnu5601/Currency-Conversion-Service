package org.project.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.validation.ConstraintViolationException;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiExceptionMappers {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMappers.class);

    private static Response.ResponseBuilder errorBody(Response.Status status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.getStatusCode());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return Response.status(status).entity(body);
    }
    @Provider
    public static class NotFoundMapper implements ExceptionMapper<jakarta.ws.rs.NotFoundException> {
        @Override
        public Response toResponse(jakarta.ws.rs.NotFoundException exception) {
            return errorBody(Response.Status.NOT_FOUND, "The requested resource does not exist.").build();
        }
    }

    @Provider
    public static class JsonMappingExceptionMapper
            implements ExceptionMapper<com.fasterxml.jackson.databind.exc.MismatchedInputException> {
        @Override
        public Response toResponse(com.fasterxml.jackson.databind.exc.MismatchedInputException exception) {
            return errorBody(Response.Status.BAD_REQUEST,
                    "Malformed request body: check that field types match (e.g. amountUSD must be a number).").build();
        }
    }

    @Provider
    public static class FallbackExceptionMapper implements ExceptionMapper<Throwable> {
        @Override
        public Response toResponse(Throwable exception) {
            LOG.error("Unhandled exception", exception);
            return errorBody(Response.Status.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred. Please try again or contact support if the problem persists.").build();
        }
    }

    @Provider
    public static class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {
        @Override
        public Response toResponse(ConstraintViolationException exception) {
            String message = exception.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return errorBody(Response.Status.BAD_REQUEST, message).build();
        }
    }
    @Provider
    public static class UnsupportedCurrencyMapper implements ExceptionMapper<UnsupportedCurrencyException> {
        @Override
        public Response toResponse(UnsupportedCurrencyException exception) {
            return errorBody(Response.Status.BAD_REQUEST, exception.getMessage()).build();
        }
    }

    @Provider
    public static class ExchangeRateUnavailableMapper implements ExceptionMapper<ExchangeRateUnavailableException> {
        @Override
        public Response toResponse(ExchangeRateUnavailableException exception) {
            LOG.error("Exchange rate provider call failed", exception);
            return errorBody(Response.Status.BAD_GATEWAY,
                    "Could not retrieve exchange rates from the upstream provider. Please try again shortly.").build();
        }
    }
}
