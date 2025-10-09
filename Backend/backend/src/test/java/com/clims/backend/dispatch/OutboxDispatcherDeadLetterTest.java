package com.clims.backend.dispatch;

import com.clims.backend.config.OutboxProperties;
import com.clims.backend.model.OutboxEvent;
import com.clims.backend.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxDispatcherDeadLetterTest {

    @Autowired
    OutboxEventRepository repo;
    OutboxDispatcher dispatcher;

    OutboxProperties props = new OutboxProperties();

    @Test
    void exceedingMaxAttempts_marksFailedPermanent() {
    props.setMaxAttempts(3); // allow 2 retries then fail on third attempt
        props.setInitialBackoffMs(1);
        props.setBackoffMultiplier(1.0); // deterministic
        props.setBatchSize(10);

        // Create failing handler always throwing
        OutboxEventHandler failing = new OutboxEventHandler() {
            @Override public String getEventType() { return "AlwaysFails"; }
            @Override public void handle(OutboxEvent event) { throw new RuntimeException("boom"); }
        };

    dispatcher = new OutboxDispatcher(repo, new SimpleMeterRegistry(), props, java.util.List.of(failing));

        OutboxEvent e = new OutboxEvent("Asset","1","AlwaysFails","{}",null,null);
        repo.save(e);

        // First attempt -> RETRY (attemptCount=1)
        dispatcher.dispatchBatch();
        e = repo.findAll().get(0);
        assertThat(e.getStatus()).isEqualTo(OutboxEvent.Status.RETRY);
        assertThat(e.getAttemptCount()).isEqualTo(1);
    forceNextAttemptNow(e);
    repo.save(e);

        // Second attempt -> RETRY (attemptCount=2)
        dispatcher.dispatchBatch();
        e = repo.findAll().get(0);
        assertThat(e.getStatus()).isEqualTo(OutboxEvent.Status.RETRY);
        assertThat(e.getAttemptCount()).isEqualTo(2);
    forceNextAttemptNow(e);
    repo.save(e);

        // Third attempt reaches maxAttempts -> FAILED (attemptCount=3)
        dispatcher.dispatchBatch();
        e = repo.findAll().get(0);
        assertThat(e.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(e.getAttemptCount()).isEqualTo(3);
        assertThat(e.getNextAttemptAt()).isNull();
    }
    private void forceNextAttemptNow(OutboxEvent e) {
        try {
            Field f = OutboxEvent.class.getDeclaredField("nextAttemptAt");
            f.setAccessible(true);
            f.set(e, Instant.now().minusSeconds(1));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
