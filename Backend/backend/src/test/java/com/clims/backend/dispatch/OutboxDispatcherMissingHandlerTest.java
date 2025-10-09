package com.clims.backend.dispatch;

import com.clims.backend.model.OutboxEvent;
import com.clims.backend.repository.OutboxEventRepository;
import com.clims.backend.config.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxDispatcherMissingHandlerTest {

    @Autowired
    OutboxEventRepository repo;
    OutboxDispatcher dispatcher;
    MeterRegistry registry;

    @BeforeEach
    void init() {
    registry = new SimpleMeterRegistry();
    dispatcher = new OutboxDispatcher(repo, registry, new OutboxProperties(), List.of());
    }

    @Test
    void unknownEventType_marksFailed() {
        OutboxEvent e = new OutboxEvent("Asset","1","NonexistentType","{}",null,null);
        repo.save(e);
        dispatcher.dispatchBatch();
        OutboxEvent persisted = repo.findById(e.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(persisted.getAttemptCount()).isEqualTo(1); // immediately failed permanent
        // metric assertion
        double missing = registry.get("outbox.dispatch.missingHandler").counter().count();
        assertThat(missing).isEqualTo(1.0);
    }
}
