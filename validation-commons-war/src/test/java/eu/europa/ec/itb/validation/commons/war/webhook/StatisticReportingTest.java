package eu.europa.ec.itb.validation.commons.war.webhook;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import eu.europa.ec.itb.validation.commons.web.WebServiceContextProvider;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticReportingTest extends BaseTest {

    private ApplicationConfig appConfig;
    private WebHook webHook;
    private DatabaseReader databaseReader;

    @Override
    @BeforeEach
    protected void setup() throws IOException {
        super.setup();
        appConfig = mock(ApplicationConfig.class);
        webHook = mock(WebHook.class);
        databaseReader = mock(DatabaseReader.class);
    }

    @Override
    @AfterEach
    protected void teardown() {
        super.teardown();
    }

    StatisticReporting createReporter(boolean initErrorIsExpected) throws Exception {
        var reporter = new StatisticReporting() {};
        var configField = StatisticReporting.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(reporter, appConfig);
        var webhookField = StatisticReporting.class.getDeclaredField("webHook");
        webhookField.setAccessible(true);
        webhookField.set(reporter, webHook);
        if (initErrorIsExpected) {
            try {
                reporter.init();
            } finally {
                var readerField = StatisticReporting.class.getDeclaredField("reader");
                readerField.setAccessible(true);
                readerField.set(reporter, databaseReader);
                appConfig.getWebhook().setStatisticsEnableCountryDetection(Boolean.TRUE);
            }
        } else {
            reporter.init();
        }
        return reporter;
    }

    @Test
    void testInitWithoutCountryExtraction() throws Exception {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatisticsEnableCountryDetection(false);
            return webhookConfig;
        });
        doAnswer((Answer<?>) invocation -> {
            var data = invocation.getArgument(0, UsageData.class);
            assertNotNull(data);
            assertEquals("validator1", data.getValidator());
            assertEquals("domain1", data.getDomain());
            assertEquals("REST", data.getApi());
            assertEquals("type1", data.getValidationType());
            assertEquals(UsageData.Result.SUCCESS, data.getResult());
            assertNull(data.getCountry());
            return null;
        }).when(webHook).sendUsageData(any(UsageData.class));
        var reporter = createReporter(false);
        reporter.sendUsageData("validator1", "domain1", "REST", "type1", UsageData.Result.SUCCESS, null);
        verify(webHook, times(1)).sendUsageData(any(UsageData.class));
    }

    @Test
    void testInitWithCountryExtraction() throws Exception {
        var dbFile = Path.of(tmpFolder.toString(), "db.mmdb");
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatisticsEnableCountryDetection(true);
            webhookConfig.setStatisticsCountryDetectionDbFile(dbFile.toString());
            return webhookConfig;
        });
        doAnswer((Answer<?>) invocation -> {
            var data = invocation.getArgument(0, UsageData.class);
            assertNotNull(data);
            assertEquals("validator1", data.getValidator());
            assertEquals("domain1", data.getDomain());
            assertEquals("REST", data.getApi());
            assertEquals("type1", data.getValidationType());
            assertEquals(UsageData.Result.SUCCESS, data.getResult());
            assertEquals("BE", data.getCountry());
            return null;
        }).when(webHook).sendUsageData(any(UsageData.class));
        when(databaseReader.country(any())).thenAnswer((Answer<?>) invocation -> {
            var country = new Country(List.of("fr-BE"), 1, 1L, true, "BE", new HashMap<>());
            return new CountryResponse(null, country, null, country, null, null);
        });
        var reporter = createReporter(true);
        reporter.sendUsageData("validator1", "domain1", "REST", "type1", UsageData.Result.SUCCESS, "127.0.0.1");
        verify(webHook, times(1)).sendUsageData(any(UsageData.class));
    }

    @Test
    void testExtractIpAddressWithHeader() throws Exception {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setIpHeader("HEADER");
            return webhookConfig;
        });
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("HEADER")).thenReturn("127.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.2");
        var reporter = createReporter(false);
        assertEquals("127.0.0.1", reporter.extractIpAddress(request));
    }

    @Test
    void testExtractIpAddressWithoutHeader() throws Exception {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setIpHeader("HEADER");
            return webhookConfig;
        });
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("HEADER")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.2");
        var reporter = createReporter(false);
        assertEquals("127.0.0.2", reporter.extractIpAddress(request));
    }

    private JoinPoint joinPointForRequestLookup() {
        var joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenAnswer((Answer<?>) invocation -> {
            var signature = mock(MethodSignature.class);
            when(signature.getParameterTypes()).thenReturn(new Class[] {String.class, Integer.class, HttpServletRequest.class, Double.class});
            return signature;
        });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "string", 1, mock(HttpServletRequest.class), 2.0D });
        return joinPoint;
    }

    @Test
    void testGetHttpRequest() throws Exception {
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> new ApplicationConfig.Webhook());
        var joinPoint = joinPointForRequestLookup();
        var reporter = createReporter(false);
        var result = reporter.getHttpRequest(joinPoint);
        assertNotNull(result);
    }

    @Test
    void testHandleUploadContext() throws Exception {
        var dbFile = Path.of(tmpFolder.toString(), "db.mmdb");
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatisticsEnableCountryDetection(true);
            webhookConfig.setStatisticsCountryDetectionDbFile(dbFile.toString());
            return webhookConfig;
        });
        var reporter = createReporter(false);
        var joinPoint = joinPointForRequestLookup();
        reporter.handleUploadContext(joinPoint, StatisticReportingConstants.WEB_API);
        var context = reporter.getAdviceContext();
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_IP));
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_API));
        assertEquals(StatisticReportingConstants.WEB_API, context.get(StatisticReportingConstants.PARAM_API));
    }

    @Test
    void testHandleSoapCallContext() throws Exception {
        var dbFile = Path.of(tmpFolder.toString(), "db.mmdb");
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatisticsEnableCountryDetection(true);
            webhookConfig.setStatisticsCountryDetectionDbFile(dbFile.toString());
            return webhookConfig;
        });
        var reporter = createReporter(false);
        var joinPoint = joinPointForRequestLookup();
        var target = mock(WebServiceContextProvider.class);
        var webServiceContext = mock(WebServiceContext.class);
        var messageContext = mock(MessageContext.class);
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("HEADER")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.2");
        when(messageContext.get(MessageContext.SERVLET_REQUEST)).thenReturn(request);
        when(webServiceContext.getMessageContext()).thenReturn(messageContext);
        when(target.getWebServiceContext()).thenReturn(webServiceContext);
        when(joinPoint.getTarget()).thenReturn(target);

        reporter.handleSoapCallContext(joinPoint);
        var context = reporter.getAdviceContext();
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_IP));
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_API));
        assertEquals(StatisticReportingConstants.SOAP_API, context.get(StatisticReportingConstants.PARAM_API));
    }

    @Test
    void testHandleRestCallContext() throws Exception {
        var dbFile = Path.of(tmpFolder.toString(), "db.mmdb");
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatisticsEnableCountryDetection(true);
            webhookConfig.setStatisticsCountryDetectionDbFile(dbFile.toString());
            return webhookConfig;
        });
        var reporter = createReporter(false);
        var joinPoint = joinPointForRequestLookup();
        reporter.handleRestCallContext(joinPoint);
        var context = reporter.getAdviceContext();
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_IP));
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_API));
        assertEquals(StatisticReportingConstants.REST_API, context.get(StatisticReportingConstants.PARAM_API));
    }

    @Test
    void testHandleEmailContext() throws Exception {
        var dbFile = Path.of(tmpFolder.toString(), "db.mmdb");
        when(appConfig.getWebhook()).thenAnswer((Answer<?>) invocation -> {
            var webhookConfig = new ApplicationConfig.Webhook();
            webhookConfig.setStatisticsEnableCountryDetection(true);
            webhookConfig.setStatisticsCountryDetectionDbFile(dbFile.toString());
            return webhookConfig;
        });
        var reporter = createReporter(false);
        var joinPoint = joinPointForRequestLookup();
        reporter.handleEmailContext(joinPoint);
        var context = reporter.getAdviceContext();
        assertTrue(context.containsKey(StatisticReportingConstants.PARAM_API));
        assertEquals(StatisticReportingConstants.EMAIL_API, context.get(StatisticReportingConstants.PARAM_API));
    }

}
