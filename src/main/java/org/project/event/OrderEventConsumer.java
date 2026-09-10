package org.project.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.project.client.OpenSearchClient;

@ApplicationScoped
public class OrderEventConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @RestClient
    OpenSearchClient openSearchClient;

    @Incoming("order-events-in")
    public void consume(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            openSearchClient.indexOrder(event.orderId, event)
                    .subscribe().with(
                            response -> LOG.infof("Indexed order %d into OpenSearch (status %d)",
                                    event.orderId, response.getStatus()),
                            failure -> LOG.error("Failed to index order into OpenSearch", failure)
                    );
        } catch (Exception e) {
            LOG.error("Failed to process order-events message: " + message, e);
        }
    }
}
