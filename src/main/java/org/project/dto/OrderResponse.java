package org.project.dto;

import org.project.model.Order;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderResponse {
    public Long orderId;
    public String customerId;
    public BigDecimal amountUSD;
    public String targetCurrency;
    public BigDecimal convertedAmount;
    public String status;
    public Instant createdAt;

    public static OrderResponse from(Order order) {
        OrderResponse response = new OrderResponse();
        response.orderId = order.id;
        response.customerId = order.customerId;
        response.amountUSD = order.amountUSD;
        response.targetCurrency = order.targetCurrency;
        response.convertedAmount = order.convertedAmount;
        response.status = order.status;
        response.createdAt = order.createdAt;
        return response;
    }
}
