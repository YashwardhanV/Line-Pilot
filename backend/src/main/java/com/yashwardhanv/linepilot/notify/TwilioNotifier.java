package com.yashwardhanv.linepilot.notify;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Sends through Twilio's Messages API. If the sender is "whatsapp:+1415...", the message goes out
 * on WhatsApp; otherwise it is an SMS.
 */
public class TwilioNotifier implements CustomerNotifier {

    private final RestClient restClient;
    private final String accountSid;
    private final String from;

    public TwilioNotifier(RestClient.Builder builder, String accountSid, String authToken, String from) {
        this.restClient = builder
                .baseUrl("https://api.twilio.com")
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
        this.accountSid = accountSid;
        this.from = from;
    }

    @Override
    public void send(String phone, String message) {
        boolean whatsapp = from.startsWith("whatsapp:");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", whatsapp ? "whatsapp:" + phone : phone);
        form.add("From", from);
        form.add("Body", message);

        restClient.post()
                .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
