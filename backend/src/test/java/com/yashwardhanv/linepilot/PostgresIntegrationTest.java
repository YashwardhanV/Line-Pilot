package com.yashwardhanv.linepilot;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.yashwardhanv.linepilot.repository.QueueTokenRepository;
import com.yashwardhanv.linepilot.repository.ServiceQueueRepository;
import com.yashwardhanv.linepilot.repository.UserAccountRepository;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("linepilot_test")
            .withUsername("linepilot")
            .withPassword("linepilot_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    QueueTokenRepository tokenRepository;

    @Autowired
    ServiceQueueRepository queueRepository;

    @Autowired
    UserAccountRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        tokenRepository.deleteAll();
        queueRepository.deleteAll();
        userRepository.deleteAll();
    }
}
