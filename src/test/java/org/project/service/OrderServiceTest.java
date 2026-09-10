package org.project.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.mockito.MockitoConfig;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.project.client.ExchangeRateClient;
import org.project.dto.ExchangeRateResponse;
import org.project.dto.OrderRequest;
import org.project.dto.OrderResponse;
import org.project.event.OrderEventProducer;
import org.project.exception.ExchangeRateUnavailableException;
import org.project.exception.UnsupportedCurrencyException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class OrderServiceTest {

    @InjectMock
    @MockitoConfig(convertScopes = true)   // add this line
    @RestClient
    ExchangeRateClient exchangeRateClient;

    @InjectMock
    OrderEventProducer orderEventProducer;

    @Inject
    OrderService orderService;

    private ExchangeRateResponse successResponse(Map<String, BigDecimal> rates) {
        ExchangeRateResponse response = new ExchangeRateResponse();
        response.result = "success";
        response.baseCode = "USD";
        response.rates = rates;
        return response;
    }

    @Test
    void convertsAmountUsingCorrectRateAndRoundsToTwoDecimals() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.8634"));
        when(exchangeRateClient.getLatestRates()).thenReturn(Uni.createFrom().item(successResponse(rates)));

        OrderRequest request = new OrderRequest();
        request.customerId = "CUST-TEST-1";
        request.amountUSD = new BigDecimal("150.00");
        request.targetCurrency = "eur"; // lower case on purpose, must be normalized

        OrderResponse response = orderService.createOrder(request);

        assertEquals("EUR", response.targetCurrency, "targetCurrency should be normalized to uppercase");
        assertEquals(new BigDecimal("129.51"), response.convertedAmount);
        assertEquals("PROCESSED", response.status);
        assertNotNull(response.orderId);

        verify(orderEventProducer, times(1)).publish(any());
    }

    @Test
    void throwsUnsupportedCurrencyWhenRateMissing() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.86"));
        when(exchangeRateClient.getLatestRates()).thenReturn(Uni.createFrom().item(successResponse(rates)));

        OrderRequest request = new OrderRequest();
        request.customerId = "CUST-TEST-2";
        request.amountUSD = new BigDecimal("100.00");
        request.targetCurrency = "ZZZ";

        assertThrows(UnsupportedCurrencyException.class, () -> orderService.createOrder(request));
        verify(orderEventProducer, never()).publish(any());
    }

    @Test
    void throwsExchangeRateUnavailableWhenProviderReportsFailure() {
        ExchangeRateResponse failure = new ExchangeRateResponse();
        failure.result = "error";
        when(exchangeRateClient.getLatestRates()).thenReturn(Uni.createFrom().item(failure));

        OrderRequest request = new OrderRequest();
        request.customerId = "CUST-TEST-3";
        request.amountUSD = new BigDecimal("50.00");
        request.targetCurrency = "GBP";

        assertThrows(ExchangeRateUnavailableException.class, () -> orderService.createOrder(request));
    }

    @Test
    void throwsExchangeRateUnavailableWhenClientCallFails() {
        when(exchangeRateClient.getLatestRates())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("connection refused")));

        OrderRequest request = new OrderRequest();
        request.customerId = "CUST-TEST-4";
        request.amountUSD = new BigDecimal("20.00");
        request.targetCurrency = "JPY";

        assertThrows(ExchangeRateUnavailableException.class, () -> orderService.createOrder(request));
    }

    @Test
    void roundsHalfUpCorrectly() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("XAF", new BigDecimal("0.125"));
        when(exchangeRateClient.getLatestRates()).thenReturn(Uni.createFrom().item(successResponse(rates)));

        OrderRequest request = new OrderRequest();
        request.customerId = "CUST-TEST-5";
        request.amountUSD = new BigDecimal("1.00");
        request.targetCurrency = "XAF";

        OrderResponse response = orderService.createOrder(request);

        // 1.00 * 0.125 = 0.125 -> HALF_UP rounds to 0.13
        assertEquals(new BigDecimal("0.13"), response.convertedAmount);
    }
}
