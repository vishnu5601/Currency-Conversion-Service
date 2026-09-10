package org.project.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventProducer {

    private static final Logger LOG = Logger.getLogger(OrderEventProducer.class);

    @Inject
    @Channel("order-events")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    public void publish(OrderCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(json);
            LOG.infof("Published ORDER_CREATED event for order %d", event.orderId);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize order event", e);
        }
    }
}
