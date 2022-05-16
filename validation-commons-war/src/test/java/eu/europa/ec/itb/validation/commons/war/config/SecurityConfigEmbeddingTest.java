package eu.europa.ec.itb.validation.commons.war.config;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = { SecurityConfigEmbeddingTest.TestConfig.class, SecurityConfig.class })
class SecurityConfigEmbeddingTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ApplicationConfig appConfig() {
            return mock(ApplicationConfig.class);
        }
        @Bean
        WebDomainConfigCache<?> webDomainConfigCache() {
            var cache = mock(WebDomainConfigCache.class);
            var domainConfig = mock(WebDomainConfig.class);
            when(domainConfig.isSupportUserInterfaceEmbedding()).thenReturn(true);
            when(cache.getAllDomainConfigurations()).thenAnswer(call -> List.of(domainConfig));
            when(cache.getConfigForDomainName(anyString())).thenReturn(domainConfig);
            return cache;
        }
        @Controller
        static class TestController {
            @RequestMapping(method= RequestMethod.GET, path = "/test1/upload")
            public @ResponseBody String test1() {
                return "TEST1";
            }
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void testConfig() throws Exception {
        mvc.perform(get("/test1/upload"))
                .andExpect(status().isOk())
                .andExpect(content().string("TEST1"))
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }

}
