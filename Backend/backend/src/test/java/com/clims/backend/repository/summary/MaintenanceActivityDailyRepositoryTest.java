package com.clims.backend.repository.summary;

import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.model.summary.MaintenanceActivityDaily;
import com.clims.backend.model.summary.MaintenanceActivityDailyId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MaintenanceActivityDailyRepositoryTest {

    @Autowired
    MaintenanceActivityDailyRepository repo;

    @Test
    void persistsAndQueriesByDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        repo.save(new MaintenanceActivityDaily(
                new MaintenanceActivityDailyId(yesterday, MaintenanceStatus.REPORTED), 3L));
        repo.save(new MaintenanceActivityDaily(
                new MaintenanceActivityDailyId(today, MaintenanceStatus.RESOLVED), 1L));

        List<MaintenanceActivityDaily> results = repo.findAllById_BucketDateBetweenOrderById_BucketDateAsc(yesterday, today);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId().getBucketDate()).isEqualTo(yesterday);
        assertThat(results.get(1).getId().getBucketDate()).isEqualTo(today);
    }
}
