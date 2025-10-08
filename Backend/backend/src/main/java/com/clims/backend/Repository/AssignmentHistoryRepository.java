package com.clims.backend.repository;

import com.clims.backend.model.AssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, Long> {
	Optional<AssignmentHistory> findTopByAsset_IdOrderByAssignedAtDesc(Long assetId);
	List<AssignmentHistory> findByAsset_IdOrderByAssignedAtDesc(Long assetId);
}
