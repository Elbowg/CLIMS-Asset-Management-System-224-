package com.clims.backend.dispatch;

import com.clims.backend.model.OutboxEvent;
import com.clims.backend.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.clims.backend.config.OutboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
// removed @Import to manually construct dispatcher and avoid requiring full MeterRegistry bean

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxDispatcherTest {

    @Autowired
    OutboxEventRepository repo;

    OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        OutboxProperties props = new OutboxProperties();
        // default values fine for test
        dispatcher = new OutboxDispatcher(repo, new SimpleMeterRegistry(), props, java.util.List.of(new TestHandler()));
    }

    @Test
    void dispatchesNewEventAndMarksSent() {
        OutboxEvent e = new OutboxEvent("Asset","1","TestEvent","{}",null,null);
        repo.save(e);
        dispatcher.dispatchBatch();
        List<OutboxEvent> all = repo.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getStatus()).isEqualTo(OutboxEvent.Status.SENT);
    }

    static class TestHandler implements OutboxEventHandler {
        @Override public String getEventType() { return "TestEvent"; }
        @Override public void handle(OutboxEvent event) { /* no-op success */ }
    }
}
