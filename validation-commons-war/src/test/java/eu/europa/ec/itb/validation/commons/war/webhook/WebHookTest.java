package eu.europa.ec.itb.validation.commons.war.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.war.webhook.UsageData;
import eu.europa.ec.itb.validation.commons.war.webhook.WebHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebHookTest {

    private ApplicationConfig appConfig;
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        appConfig = mock(ApplicationConfig.class);
        restTemplate = mock(RestTemplate.class);
    }

    WebHook createWebHook() throws Exception {
        var webhook = new WebHook();
        var configField = WebHook.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(webhook, appConfig);
        var objectMapperField = WebHook.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(webhook, new ObjectMapper());
        webhook.initializeAttributes();
        var restTemplateField = WebHook.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(webhook, restTemplate);
        return webhook;
    }

    @Test
    void testInitOk() {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatistics("http://test.com");
            return webhookConfig;
        });
        assertDoesNotThrow(this::createWebHook);
    }

    @Test
    void testInitBadURL() {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatistics("__BAD__");
            return webhookConfig;
        });
        assertThrows(MalformedURLException.class, this::createWebHook);
    }

    @Test
    void testSendUsageData() throws Exception {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatistics("http://test.com");
            return webhookConfig;
        });
        when(restTemplate.postForEntity(any(), any(), any(), any(Map.class))).thenAnswer((Answer<?>) invocation -> {
            var entity = invocation.getArgument(1, HttpEntity.class);
            assertNotNull(entity.getBody());
            ObjectMapper mapper = new ObjectMapper();
            var json = mapper.readTree(new StringReader(entity.getBody().toString()));
            assertNotNull(json.get("validator"));
            assertNotNull(json.get("domain"));
            assertNotNull(json.get("api"));
            assertNotNull(json.get("validationType"));
            assertNotNull(json.get("result"));
            assertNotNull(json.get("validationTime"));
            assertNotNull(json.get("country"));
            assertEquals("validator1", json.get("validator").asText());
            assertEquals("domain1", json.get("domain").asText());
            assertEquals("REST", json.get("api").asText());
            assertEquals("type1", json.get("validationType").asText());
            assertEquals(UsageData.Result.SUCCESS.name(), json.get("result").asText());
            assertDoesNotThrow(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(json.get("validationTime").asText()));
            assertEquals("BE", json.get("country").asText());
            return new ResponseEntity<String>(HttpStatus.OK);
        });
        var webhook = createWebHook();
        webhook.sendUsageData(new UsageData("validator1", "domain1", "REST", "type1", UsageData.Result.SUCCESS, "BE"));
        verify(restTemplate, times(1)).postForEntity(any(), any(), any(), any(Map.class));
    }

    @Test
    void testSendUsageDataWithSecret() throws Exception {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatistics("http://test.com");
            webhookConfig.setStatisticsSecret("secret1");
            return webhookConfig;
        });
        when(restTemplate.postForEntity(any(), any(), any(), any(Map.class))).thenAnswer((Answer<?>) invocation -> {
            var params = invocation.getArgument(3, Map.class);
            assertEquals("secret1", params.get("secret"));
            return new ResponseEntity<String>(HttpStatus.OK);
        });
        var webhook = createWebHook();
        webhook.sendUsageData(new UsageData("validator1", "domain1", "REST", "type1", UsageData.Result.SUCCESS, "BE"));
        verify(restTemplate, times(1)).postForEntity(any(), any(), any(), any(Map.class));
    }
}