package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.test.BaseSpringTest;
import eu.europa.ec.itb.validation.commons.test.BaseTestConfiguration;
import eu.europa.ec.itb.validation.plugin.PluginInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DomainPluginInfoProviderTest.TestConfig.class, BaseTestConfiguration.class })
public class DomainPluginInfoProviderTest extends BaseSpringTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public DomainPluginConfigProvider<DomainConfig> domainPluginConfigProvider() {
            return new DomainPluginConfigProvider<>();
        }
    }

    @Autowired
    DomainConfigCache<DomainConfig> domainConfigCache;
    @Autowired
    DomainPluginConfigProvider<DomainConfig> provider;

    @Override
    @BeforeEach
    protected void setup() throws IOException {
        super.setup();
        reset(domainConfigCache);
    }

    @Override
    @AfterEach
    protected void teardown() {
        super.teardown();
    }

    @Test
    void testGetPluginClassifier() {
        var domainConfig = new DomainConfig();
        domainConfig.setDomainName("domainName");
        assertEquals("domainName|type1", provider.getPluginClassifier(domainConfig, "type1"));
    }

    @Test
    void testGetPluginInfoPerType() {
        List<DomainConfig> domainConfigs = new ArrayList<>();
        domainConfigs.add(new DomainConfig());
        domainConfigs.get(0).setDomain("domain1");
        domainConfigs.get(0).setDomainName("domain1");
        domainConfigs.get(0).setDefined(true);
        domainConfigs.get(0).setType(List.of("type1", "type2"));
        domainConfigs.get(0).setPluginDefaultConfig(List.of(new PluginInfo()));
        domainConfigs.get(0).getPluginDefaultConfig().get(0).setJarPath(Path.of(appConfig.getTmpFolder(), "path1"));
        domainConfigs.get(0).getPluginDefaultConfig().get(0).setPluginClasses(List.of("Plugin1", "Plugin2"));
        domainConfigs.get(0).setPluginPerTypeConfig(Map.of("type1", List.of(new PluginInfo())));
        domainConfigs.get(0).getPluginPerTypeConfig().get("type1").get(0).setJarPath(Path.of(appConfig.getTmpFolder(), "path2"));
        domainConfigs.get(0).getPluginPerTypeConfig().get("type1").get(0).setPluginClasses(List.of("Plugin3", "Plugin4"));
        when(domainConfigCache.getAllDomainConfigurations()).thenReturn(domainConfigs);
        var result = provider.getPluginInfoPerType();
        assertEquals(2, result.size());
        assertEquals(2, result.get("domain1|type1").size());

        assertEquals(Path.of(appConfig.getTmpFolder(), "path1"), result.get("domain1|type1").get(0).getJarPath());
        assertEquals(2, result.get("domain1|type1").get(0).getPluginClasses().size());
        assertTrue(result.get("domain1|type1").get(0).getPluginClasses().contains("Plugin1"));
        assertTrue(result.get("domain1|type1").get(0).getPluginClasses().contains("Plugin2"));

        assertEquals(Path.of(appConfig.getTmpFolder(), "path2"), result.get("domain1|type1").get(1).getJarPath());
        assertEquals(2, result.get("domain1|type1").get(1).getPluginClasses().size());
        assertTrue(result.get("domain1|type1").get(1).getPluginClasses().contains("Plugin3"));
        assertTrue(result.get("domain1|type1").get(1).getPluginClasses().contains("Plugin4"));

        assertEquals(1, result.get("domain1|type2").size());
        assertEquals(Path.of(appConfig.getTmpFolder(), "path1"), result.get("domain1|type2").get(0).getJarPath());
        assertEquals(2, result.get("domain1|type2").get(0).getPluginClasses().size());
        assertTrue(result.get("domain1|type2").get(0).getPluginClasses().contains("Plugin1"));
        assertTrue(result.get("domain1|type2").get(0).getPluginClasses().contains("Plugin2"));

    }
}
