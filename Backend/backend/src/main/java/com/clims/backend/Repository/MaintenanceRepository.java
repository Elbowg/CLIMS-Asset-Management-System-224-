package com.clims.backend.repository;

import com.clims.backend.model.Maintenance;
import com.clims.backend.repository.projection.MaintenanceDailyStatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    @Query("select FUNCTION('date', m.reportedAt) as day, m.status as status, count(m) as count " +
	    "from Maintenance m " +
	    "where m.reportedAt between :from and :to " +
	    "group by FUNCTION('date', m.reportedAt), m.status " +
	    "order by day asc")
    List<MaintenanceDailyStatusCount> findDailyStatusCounts(@Param("from") LocalDateTime from,
								   @Param("to") LocalDateTime to);
}