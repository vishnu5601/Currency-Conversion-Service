package org.project.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRateResponse {
    public String result;

    @JsonProperty("base_code")
    public String baseCode;

    @JsonProperty("time_last_update_utc")
    public String timeLastUpdateUtc;

    public Map<String, BigDecimal> rates;
}
