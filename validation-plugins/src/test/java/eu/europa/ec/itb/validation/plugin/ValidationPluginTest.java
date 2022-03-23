package eu.europa.ec.itb.validation.plugin;

import com.gitb.core.Metadata;
import com.gitb.core.ValidationModule;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.Void;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationPluginTest {

    ValidationPlugin createPlugin(Supplier<GetModuleDefinitionResponse> responseFn) {
        return new ValidationPlugin() {
            @Override
            public GetModuleDefinitionResponse getModuleDefinition(Void aVoid) {
                return responseFn.get();
            }
            @Override
            public ValidationResponse validate(ValidateRequest validateRequest) {
                return new ValidationResponse();
            }
        };
    }

    @Test
    void testGetNameForNull() {
        assertEquals("", createPlugin(() -> null).getName());
        assertEquals("", createPlugin(GetModuleDefinitionResponse::new).getName());
        assertEquals("", createPlugin(() -> {
            var response = new GetModuleDefinitionResponse();
            response.setModule(new ValidationModule());
            return response;
        }).getName());
        assertEquals("id1", createPlugin(() -> {
            var response = new GetModuleDefinitionResponse();
            response.setModule(new ValidationModule());
            response.getModule().setId("id1");
            return response;
        }).getName());
        assertEquals("", createPlugin(() -> {
            var response = new GetModuleDefinitionResponse();
            response.setModule(new ValidationModule());
            response.getModule().setMetadata(new Metadata());
            return response;
        }).getName());
        assertEquals("name1", createPlugin(() -> {
            var response = new GetModuleDefinitionResponse();
            response.setModule(new ValidationModule());
            response.getModule().setMetadata(new Metadata());
            response.getModule().getMetadata().setName("name1");
            return response;
        }).getName());
    }

}
