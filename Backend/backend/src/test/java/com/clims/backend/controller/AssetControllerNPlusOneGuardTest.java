package com.clims.backend.controller;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.User;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
class AssetControllerNPlusOneGuardTest {

    @Autowired MockMvc mockMvc;
    @Autowired AssetRepository assetRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setup() {
        assetRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User();
        u.setUsername("n1guard");
        u.setPassword("x");
        u.setEmail("n1@guard.test");
        User saved = userRepository.save(u);

        // Seed a reasonable number of rows
        IntStream.range(0, 30).forEach(i -> {
            Asset a = new Asset();
            a.setName("Guard-" + i);
            a.setSerialNumber("GSN" + i);
            a.setPurchaseDate(LocalDate.now());
            a.setStatus(i % 3 == 0 ? AssetStatus.AVAILABLE : AssetStatus.RETIRED);
            if (i == 3) {
                a.setAssignedUser(saved);
                a.setStatus(AssetStatus.ASSIGNED);
            }
            assetRepository.save(a);
        });

        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear(); // clear counts after setup; only count queries from the test request
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void listing_uses_single_select_plus_count_only() throws Exception {
        // Exercise: default listing (page 0, size 20) using projection
        mockMvc.perform(get("/api/assets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Hibernate reports HQL/JPQL query executions. We expect exactly 2: page content + count.
        long queryCount = stats.getQueryExecutionCount();

        assertThat(queryCount)
                .withFailMessage("N+1 detected: expected <=2 queries for listing (select + count), but was %d", queryCount)
                .isLessThanOrEqualTo(2);
    }
}
