package com.clims.backend.repository;

import com.clims.backend.model.Asset;
import com.clims.backend.dto.AssetListProjection;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.repository.projection.AssetDailyStatusCount;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySerialNumber(String serialNumber);

    // Projection-based page (avoids loading full entity graph for simple list)
    Page<AssetListProjection> findAllByOrderByIdAsc(Pageable pageable);

    @Query("select a.id as id, a.name as name, a.status as status, a.assignedUser.id as assignedUserId " +
            "from Asset a " +
            "where (:status is null or a.status = :status) " +
            "and (:assignedUserId is null or a.assignedUser.id = :assignedUserId)")
    Page<AssetListProjection> findFiltered(@Param("status") AssetStatus status,
                                           @Param("assignedUserId") Long assignedUserId,
                                           Pageable pageable);

    // Aggregate assets by status per day using purchaseDate as the day bucket.
    // Counts assets whose purchaseDate falls within the window. This acts as a proxy for daily additions.
    @Query("select a.purchaseDate as day, a.status as status, count(a) as count " +
            "from Asset a " +
            "where a.purchaseDate between :from and :to " +
            "group by a.purchaseDate, a.status " +
            "order by day asc")
    java.util.List<AssetDailyStatusCount> findDailyStatusCounts(@Param("from") LocalDate from,
                                                                @Param("to") LocalDate to);
}
