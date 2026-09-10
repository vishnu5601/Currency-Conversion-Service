package org.project.client;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.project.dto.ExchangeRateResponse;

@RegisterRestClient(configKey = "exchange-rate-api")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface ExchangeRateClient {

    @GET
    @Path("/v6/latest/USD")
    Uni<ExchangeRateResponse> getLatestRates();
}
