package com.clims.backend.repository;

import com.clims.backend.model.OutboxEvent;
import com.clims.backend.model.OutboxEvent.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("select e from OutboxEvent e where (e.status = 'NEW' or e.status = 'RETRY') and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) order by e.id asc")
    List<OutboxEvent> findDispatchBatch(@Param("now") Instant now, Pageable pageable);

    long countByStatus(Status status);
}
