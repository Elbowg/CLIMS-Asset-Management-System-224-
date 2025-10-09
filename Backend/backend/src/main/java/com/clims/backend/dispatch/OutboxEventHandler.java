package com.clims.backend.dispatch;

import com.clims.backend.model.OutboxEvent;

/**
 * Contract for handling a specific outbox event type. Implementations should be idempotent.
 */
public interface OutboxEventHandler {
    /** @return event type this handler supports (e.g. "AssetCreated"). */
    String getEventType();

    /** Process the given event. Throwing an exception triggers retry logic. */
    void handle(OutboxEvent event) throws Exception;
}
