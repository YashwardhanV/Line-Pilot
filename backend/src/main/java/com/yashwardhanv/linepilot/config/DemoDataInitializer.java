package com.yashwardhanv.linepilot.config;

import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.repository.QueueTokenRepository;
import com.yashwardhanv.linepilot.repository.ServiceQueueRepository;
import com.yashwardhanv.linepilot.repository.UserAccountRepository;
import com.yashwardhanv.linepilot.service.QueueService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final UserAccountRepository userRepository;
    private final ServiceQueueRepository queueRepository;
    private final QueueTokenRepository tokenRepository;
    private final QueueService queueService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataInitializer(UserAccountRepository userRepository,
                               ServiceQueueRepository queueRepository,
                               QueueTokenRepository tokenRepository,
                               QueueService queueService,
                               PasswordEncoder passwordEncoder,
                               Clock clock) {
        this.userRepository = userRepository;
        this.queueRepository = queueRepository;
        this.tokenRepository = tokenRepository;
        this.queueService = queueService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        createUser("staff1", "Anika Rao", "Counter 1");
        createUser("staff2", "Kabir Shah", "Counter 2");

        ServiceQueue queue = queueRepository.findByCode("GENERAL")
                .orElseGet(() -> queueRepository.save(new ServiceQueue(
                        "GENERAL", "Citizen Service Desk", "Ground floor · Counter hall",
                        "A", 6)));
        queueRepository.findByCode("DOCS")
                .orElseGet(() -> queueRepository.save(new ServiceQueue(
                        "DOCS", "Document Verification", "First floor · Room 4",
                        "D", 4)));

        if (tokenRepository.countByServiceQueueIdAndServiceDate(
                queue.getId(), LocalDate.now(clock)) == 0) {
            queueService.joinQueue(queue.getId(), "Demo Customer 1");
            queueService.joinQueue(queue.getId(), "Demo Customer 2");
            queueService.joinQueue(queue.getId(), "Demo Customer 3");
        }
    }

    private void createUser(String username, String displayName, String counterLabel) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseGet(() -> new UserAccount(
                        username, passwordEncoder.encode("demo123"), displayName, counterLabel));
        if (user.getCounterLabel() == null) {
            user.assignCounter(counterLabel); // demo users created before counters existed
        }
        userRepository.save(user);
    }
}
