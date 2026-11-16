package com.yashwardhanv.linepilot.notify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationConfig {

    @Bean
    @ConditionalOnProperty(name = "app.notify.provider", havingValue = "twilio")
    CustomerNotifier twilioNotifier(RestClient.Builder builder,
                                    @Value("${app.notify.twilio.account-sid}") String accountSid,
                                    @Value("${app.notify.twilio.auth-token}") String authToken,
                                    @Value("${app.notify.twilio.from}") String from) {
        return new TwilioNotifier(builder, accountSid, authToken, from);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notify.provider", havingValue = "log", matchIfMissing = true)
    CustomerNotifier loggingNotifier() {
        return new LoggingNotifier();
    }
}
