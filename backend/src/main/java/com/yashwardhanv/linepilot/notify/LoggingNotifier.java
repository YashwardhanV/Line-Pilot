package com.yashwardhanv.linepilot.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default for local runs: writes the message to the log instead of sending it. */
public class LoggingNotifier implements CustomerNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

    @Override
    public void send(String phone, String message) {
        log.info("[notify] to {}: {}", mask(phone), message);
    }

    static String mask(String phone) {
        return phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
