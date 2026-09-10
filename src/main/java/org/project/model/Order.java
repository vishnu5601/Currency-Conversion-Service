package org.project.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {
    @Column(name = "customer_id", nullable = false, length = 64)
    public String customerId;

    @Column(name = "amount_usd", nullable = false, precision = 19, scale = 2)
    public BigDecimal amountUSD;

    @Column(name = "target_currency", nullable = false, length = 3)
    public String targetCurrency;

    @Column(name = "converted_amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal convertedAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 6)
    public BigDecimal exchangeRate;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    public Long id;
}




















