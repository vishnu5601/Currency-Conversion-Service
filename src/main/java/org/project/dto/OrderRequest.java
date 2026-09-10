package org.project.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class OrderRequest {
    @NotBlank(message = "customerId is required")
    public String customerId;

    @NotNull(message = "amountUSD is required")
    @DecimalMin(value = "0.01", message = "amountUSD must be greater than 0")
    public BigDecimal amountUSD;

    @NotBlank(message = "targetCurrency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "targetCurrency must be a 3-letter ISO currency code")
    public String targetCurrency;
}
