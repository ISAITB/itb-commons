package eu.europa.ec.itb.validation.plugin;

import com.gitb.tr.TestResultType;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.Void;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginManagerTest {

    private PluginManager createPluginManager(PluginConfigProvider provider) throws Exception {
        // Using this approach to allow mocking of the private provider before bean initialisation.
        var manager = new PluginManager();
        var providerField = PluginManager.class.getDeclaredField("configProvider");
        providerField.setAccessible(true);
        providerField.set(manager, provider);
        var initMethod = PluginManager.class.getDeclaredMethod("loadPlugins");
        initMethod.setAccessible(true);
        initMethod.invoke(manager);
        return manager;
    }

    @Test
    void testPluginLoadingEmptyPluginList() throws Exception {
        var provider = mock(PluginConfigProvider.class);
        when(provider.getPluginInfoPerType()).thenReturn(Map.of("domain1|type1", Collections.emptyList()));
        var result = createPluginManager(provider).getPlugins("domain1|type1");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testPluginLoadingNoClassifier() throws Exception {
        var provider = mock(PluginConfigProvider.class);
        when(provider.getPluginInfoPerType()).thenReturn(Map.of("domain1|type1", Collections.emptyList()));
        var result = createPluginManager(provider).getPlugins("domainX|typeX");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testPluginLoadingForBadPath() throws Exception {
        var tmpFolder = Files.createTempDirectory("itb");
        var pluginPath = Path.of(tmpFolder.toString(), "plugin.jar"); // Non-existent JAR
        var provider = mock(PluginConfigProvider.class);
        when(provider.getPluginInfoPerType()).thenAnswer((Answer<?>) invocation -> {
            var config = new HashMap<String, List<PluginInfo>>();
            config.put("domain1|type1", List.of(new PluginInfo()));
            config.get("domain1|type1").get(0).setJarPath(pluginPath);
            config.get("domain1|type1").get(0).setPluginClasses(List.of("org.test.Plugin"));
            return config;
        });
        var result = createPluginManager(provider).getPlugins("domain1|type1");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testPluginLoadingWithPlugins() throws Exception {
        var tmpFolder = Files.createTempDirectory("itb");
        var pluginPath = Path.of(tmpFolder.toString(), "plugin.jar");
        PluginManager manager = null;
        try {
            var provider = mock(PluginConfigProvider.class);
            Files.copy(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("testPlugins/plugin.jar")), pluginPath);
            when(provider.getPluginInfoPerType()).thenAnswer((Answer<?>) invocation -> {
                var config = new HashMap<String, List<PluginInfo>>();
                config.put("domain1|type1", List.of(new PluginInfo()));
                config.get("domain1|type1").get(0).setJarPath(pluginPath);
                config.get("domain1|type1").get(0).setPluginClasses(List.of("org.test.Plugin"));
                return config;
            });
            manager = createPluginManager(provider);
            var result = manager.getPlugins("domain1|type1");
            assertNotNull(result);
            assertEquals(1, result.length);
            assertEquals("id", result[0].getName());
            assertTrue(result[0] instanceof PluginAdapter);
            assertNotNull(result[0].getModuleDefinition(new Void()));
            assertNotNull(result[0].getModuleDefinition(new Void()).getModule());
            assertEquals("id", result[0].getModuleDefinition(new Void()).getModule().getId());
            assertNotNull(result[0].validate(new ValidateRequest()));
            assertNotNull(result[0].validate(new ValidateRequest()).getReport());
            assertEquals(TestResultType.SUCCESS, result[0].validate(new ValidateRequest()).getReport().getResult());
        } finally {
            if (manager != null) {
                var destroyMethod = PluginManager.class.getDeclaredMethod("destroy");
                destroyMethod.setAccessible(true);
                destroyMethod.invoke(manager);
            }
            Files.deleteIfExists(pluginPath);
            Files.deleteIfExists(tmpFolder);
        }
    }

}
