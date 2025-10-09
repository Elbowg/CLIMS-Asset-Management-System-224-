package com.clims.backend.repository;

import com.clims.backend.model.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuditEventRepositoryTest {

    @Autowired
    private AuditEventRepository repository;

    @Test
    @DisplayName("persist and retrieve audit events with timestamps set on persist")
    void persistAndRetrieve() {
        AuditEvent e1 = new AuditEvent("alice","LOGIN","success","127.0.0.1","req-1");
        AuditEvent e2 = new AuditEvent("alice","LOGOUT",null,"127.0.0.1","req-2");
        AuditEvent e3 = new AuditEvent("bob","LOGIN","success","10.0.0.5","req-3");

        repository.saveAll(List.of(e1,e2,e3));

        List<AuditEvent> all = repository.findAll();
        assertThat(all).hasSize(3);
        assertThat(all).allSatisfy(ev -> {
            assertThat(ev.getId()).isNotNull();
            assertThat(ev.getTimestamp()).isNotNull();
            assertThat(ev.getPrincipal()).isNotBlank();
            assertThat(ev.getAction()).isNotBlank();
            assertThat(ev.getRequestId()).isNotBlank();
        });
    }

    @Test
    @DisplayName("ordering by timestamp descending can be emulated in-memory now; future index justification")
    void naturalOrderingExample() throws InterruptedException {
        AuditEvent early = new AuditEvent("charlie","LOGIN",null,"127.0.0.1","r-early");
        repository.save(early);
        // ensure later timestamp (sleep small interval)
        Thread.sleep(5);
        AuditEvent later = new AuditEvent("charlie","REFRESH",null,"127.0.0.1","r-late");
        repository.save(later);

        List<AuditEvent> events = repository.findAll().stream()
                .filter(e -> e.getPrincipal().equals("charlie"))
                .sorted((a,b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();

        assertThat(events).hasSize(2);
    assertThat(events.get(0).getTimestamp()).isAfterOrEqualTo(events.get(1).getTimestamp());
    assertThat(events.get(0).getRequestId()).isEqualTo("r-late");
    }

    @Test
    @DisplayName("stored timestamps are close to current time (sanity)")
    void timestampsNearNow() {
        Instant before = Instant.now().minusSeconds(2);
        AuditEvent ev = new AuditEvent("dana","LOGIN",null,"8.8.8.8","req-x");
        repository.save(ev);
        AuditEvent persisted = repository.findById(ev.getId()).orElseThrow();
        Instant after = Instant.now().plusSeconds(2);
        assertThat(persisted.getTimestamp()).isAfter(before).isBefore(after);
    }
}
