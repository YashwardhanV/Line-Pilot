package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.notify.TwilioNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TwilioNotifierTest {

    private static final String URL = "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json";

    @Test
    void sendsWhatsAppWhenTheSenderIsAWhatsAppNumber() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic QUMxMjM6c2VjcmV0")) // AC123:secret
                .andExpect(content().formDataContains(Map.of(
                        "To", "whatsapp:+919876543210",
                        "From", "whatsapp:+14155238886",
                        "Body", "Your turn")))
                .andRespond(withSuccess());

        new TwilioNotifier(builder, "AC123", "secret", "whatsapp:+14155238886").send("+919876543210", "Your turn");

        server.verify();
    }

    @Test
    void sendsSmsOtherwise() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL))
                .andExpect(content().formDataContains(Map.of("To", "+919876543210", "From", "+15005550006")))
                .andRespond(withSuccess());

        new TwilioNotifier(builder, "AC123", "secret", "+15005550006").send("+919876543210", "Your turn");

        server.verify();
    }
}
