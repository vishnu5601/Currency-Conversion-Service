package org.project.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderCreatedEvent {

    @JsonProperty("eventType")
    public String eventType = "ORDER_CREATED";

    public Long orderId;
    public String customerId;
    public BigDecimal amountUSD;
    public String targetCurrency;
    public BigDecimal convertedAmount;
    public String status;
    public Instant createdAt;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Long orderId, String customerId, BigDecimal amountUSD,
                              String targetCurrency, BigDecimal convertedAmount,
                              String status, Instant createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amountUSD = amountUSD;
        this.targetCurrency = targetCurrency;
        this.convertedAmount = convertedAmount;
        this.status = status;
        this.createdAt = createdAt;
    }
}
