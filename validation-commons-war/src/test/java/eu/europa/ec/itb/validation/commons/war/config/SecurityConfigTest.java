package eu.europa.ec.itb.validation.commons.war.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = { SecurityConfigTest.TestConfig.class, SecurityConfig.class })
public class SecurityConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Controller
        static class TestController {
            @RequestMapping(method= RequestMethod.GET, path = "/test")
            public @ResponseBody String test() {
                return "TEST";
            }
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void testConfig() throws Exception {
        mvc.perform(get("/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("TEST"));
    }

    @Test
    void testCORS() throws Exception {
        mvc.perform(options("/test")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "http://a.domain.com"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("OPTIONS")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("HEAD")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }

}
