package com.clims.backend.repository.summary;

import com.clims.backend.model.summary.AssetStatusDaily;
import com.clims.backend.model.summary.AssetStatusDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AssetStatusDailyRepository extends JpaRepository<AssetStatusDaily, AssetStatusDailyId> {
    List<AssetStatusDaily> findAllById_BucketDateBetweenOrderById_BucketDateAsc(LocalDate from, LocalDate to);

    @Modifying
    @Query("delete from AssetStatusDaily a where a.id.bucketDate between :from and :to")
    int deleteByBucketDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
