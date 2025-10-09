package com.clims.backend.dispatch;

import com.clims.backend.config.OutboxProperties;
import com.clims.backend.model.OutboxEvent;
import com.clims.backend.model.OutboxEvent.Status;
import com.clims.backend.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({OutboxProperties.class})
class OutboxDispatcherHandlerTests {

    @Autowired
    OutboxEventRepository repo;

    @Autowired
    OutboxProperties properties;

    SimpleMeterRegistry meterRegistry;

    RecordingHandler recordingHandler;
    FailingHandler failingHandler;
    OutboxDispatcher dispatcher;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        recordingHandler = new RecordingHandler();
        failingHandler = new FailingHandler();
        dispatcher = new OutboxDispatcher(repo, meterRegistry, properties, List.of(recordingHandler, failingHandler));
    }

    @Test
    @Transactional
    void handlerSuccess_marksSent() {
        OutboxEvent e = new OutboxEvent("Asset", "1", "TestHandled", "{}", null, null);
        // adapt handler name to RecordingHandler event type
        e = repo.save(new OutboxEvent("Asset", "1", recordingHandler.getEventType(), "{}", null, null));
        dispatcher.dispatchBatch();
        OutboxEvent reloaded = repo.findById(e.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.SENT);
        assertThat(recordingHandler.invocations).isEqualTo(1);
    }

    @Test
    @Transactional
    void handlerFailure_schedulesRetry() {
        OutboxEvent e = repo.save(new OutboxEvent("Asset", "2", failingHandler.getEventType(), "{}", null, null));
        dispatcher.dispatchBatch();
        OutboxEvent reloaded = repo.findById(e.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.RETRY);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getNextAttemptAt()).isAfter(Instant.now().minusSeconds(1));
    }

    // Test handlers
    static class RecordingHandler implements OutboxEventHandler {
        int invocations = 0;
        @Override public String getEventType() { return "Recording"; }
        @Override public void handle(OutboxEvent event) { invocations++; }
    }

    static class FailingHandler implements OutboxEventHandler {
        @Override public String getEventType() { return "AlwaysFail"; }
        @Override public void handle(OutboxEvent event) { throw new RuntimeException("boom"); }
    }
}
