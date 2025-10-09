package com.clims.backend.dispatch.handlers;

import com.clims.backend.dispatch.OutboxEventHandler;
import com.clims.backend.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder handler for AssetCreated events. In future this could publish to a message broker.
 */
@Component
public class AssetCreatedHandler implements OutboxEventHandler {
    private static final Logger log = LoggerFactory.getLogger(AssetCreatedHandler.class);

    @Override
    public String getEventType() { return "AssetCreated"; }

    @Override
    public void handle(OutboxEvent event) {
        // For now just log structured line; payload already JSON.
        log.info("Handled AssetCreated event id={} aggregate={}#{} payload={}", event.getId(), event.getAggregateType(), event.getAggregateId(), event.getPayload());
    }
}
