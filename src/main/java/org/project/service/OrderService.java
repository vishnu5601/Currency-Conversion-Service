package org.project.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.project.client.ExchangeRateClient;
import org.project.dto.ExchangeRateResponse;
import org.project.dto.OrderRequest;
import org.project.dto.OrderResponse;
import org.project.event.OrderCreatedEvent;
import org.project.event.OrderEventProducer;
import org.project.exception.ExchangeRateUnavailableException;
import org.project.exception.UnsupportedCurrencyException;
import org.project.model.Order;
import org.project.model.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class OrderService {
    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private static final int SCALE = 2;
    private static final Duration RATE_LOOKUP_TIMEOUT = Duration.ofSeconds(5);

    @Inject
    @RestClient
    ExchangeRateClient exchangeRateClient;

    @Inject
    OrderEventProducer orderEventProducer;

    public OrderResponse createOrder(OrderRequest request) {
        String targetCurrency = request.targetCurrency.toUpperCase();
        ExchangeRateResponse rateResponse = fetchRates();

        if (!"success".equalsIgnoreCase(rateResponse.result)) {
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider returned a non-success result: " + rateResponse.result);
        }

        BigDecimal rate = rateResponse.rates != null ? rateResponse.rates.get(targetCurrency) : null;
        if (rate == null) {
            throw new UnsupportedCurrencyException(targetCurrency);
        }

        BigDecimal convertedAmount = request.amountUSD.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);

        Order order = new Order();
        order.customerId = request.customerId;
        order.amountUSD = request.amountUSD.setScale(SCALE, RoundingMode.HALF_UP);
        order.targetCurrency = targetCurrency;
        order.convertedAmount = convertedAmount;
        order.exchangeRate = rate;
        order.status = OrderStatus.PROCESSED.name();
        order.createdAt = Instant.now();

        QuarkusTransaction.requiringNew().run(order::persist);

        orderEventProducer.publish(new OrderCreatedEvent(
                order.id, order.customerId, order.amountUSD,
                order.targetCurrency, order.convertedAmount,
                order.status, order.createdAt));

        LOG.infof("Created order %d for customer %s: %s USD -> %s %s (rate %s)",
                order.id, order.customerId, order.amountUSD, convertedAmount, targetCurrency, rate);

        return OrderResponse.from(order);
    }
    public Order findById(Long id) {
        return Order.findById(id);
    }

    public List<OrderResponse> listOrders(String customerId) {
        List<Order> orders = (customerId != null)
                ? Order.list("customerId", customerId)
                : Order.listAll();
        return orders.stream().map(OrderResponse::from).toList();
    }

    private ExchangeRateResponse fetchRates() {
        try {
            return exchangeRateClient.getLatestRates()
                    .await().atMost(RATE_LOOKUP_TIMEOUT);
        } catch (Exception e) {
            throw new ExchangeRateUnavailableException("Failed to fetch exchange rates", e);
        }
    }
}