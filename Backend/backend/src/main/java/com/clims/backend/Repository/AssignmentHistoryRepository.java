package com.clims.backend.repository;

import com.clims.backend.model.AssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, Long> {
	Optional<AssignmentHistory> findTopByAsset_IdOrderByAssignedAtDesc(Long assetId);
}
