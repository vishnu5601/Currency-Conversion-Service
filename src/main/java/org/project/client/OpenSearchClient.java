package org.project.client;

import io.quarkus.rest.client.reactive.ClientBasicAuth;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.project.event.OrderCreatedEvent;

@RegisterRestClient(configKey = "opensearch-api")
@ClientBasicAuth(
        username = "${quarkus.rest-client.opensearch-api.username}",
        password = "${quarkus.rest-client.opensearch-api.password}"
)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpenSearchClient {

    @PUT
    @Path("/orders/_doc/{id}")
    Uni<Response> indexOrder(@PathParam("id") Long id, OrderCreatedEvent event);
}
