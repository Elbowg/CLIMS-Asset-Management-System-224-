package com.clims.backend.repository;

import com.clims.backend.model.AuditEvent;
import com.clims.backend.repository.projection.AuditDailyActionCount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

 public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
	@Modifying
	@Query(value = "delete from audit_event where timestamp < :cutoff", nativeQuery = true)
	int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Query("select FUNCTION('date', ae.timestamp) as day, ae.action as action, count(ae) as count " +
	    "from AuditEvent ae " +
	    "where ae.timestamp between :from and :to " +
	    "group by FUNCTION('date', ae.timestamp), ae.action " +
	    "order by day asc")
    java.util.List<AuditDailyActionCount> findDailyActionCounts(@Param("from") Instant from,
									 @Param("to") Instant to);
 }
