package com.yashwardhanv.linepilot.config;

import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.repository.QueueTokenRepository;
import com.yashwardhanv.linepilot.repository.ServiceQueueRepository;
import com.yashwardhanv.linepilot.repository.UserAccountRepository;
import com.yashwardhanv.linepilot.service.QueueCommandService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final UserAccountRepository userRepository;
    private final ServiceQueueRepository queueRepository;
    private final QueueTokenRepository tokenRepository;
    private final QueueCommandService commandService;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserAccountRepository userRepository,
                               ServiceQueueRepository queueRepository,
                               QueueTokenRepository tokenRepository,
                               QueueCommandService commandService,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.queueRepository = queueRepository;
        this.tokenRepository = tokenRepository;
        this.commandService = commandService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createUser("staff1", "Anika Rao");
        createUser("staff2", "Kabir Shah");

        ServiceQueue queue = queueRepository.findByCode("GENERAL")
                .orElseGet(() -> queueRepository.save(new ServiceQueue(
                        "GENERAL", "Citizen Service Desk", "Ground floor · Counter hall",
                        "A", 6)));
        queueRepository.findByCode("DOCS")
                .orElseGet(() -> queueRepository.save(new ServiceQueue(
                        "DOCS", "Document Verification", "First floor · Room 4",
                        "D", 4)));

        if (tokenRepository.countByServiceQueueIdAndServiceDate(
                queue.getId(), java.time.LocalDate.now(java.time.ZoneOffset.UTC)) == 0) {
            commandService.joinQueue(queue.getId(), "Demo Customer 1");
            commandService.joinQueue(queue.getId(), "Demo Customer 2");
            commandService.joinQueue(queue.getId(), "Demo Customer 3");
        }
    }

    private void createUser(String username, String displayName) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new UserAccount(
                    username, passwordEncoder.encode("demo123"), displayName));
        }
    }
}
