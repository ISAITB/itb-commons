package eu.europa.ec.itb.validation.plugin;

import com.gitb.core.AnyContent;
import com.gitb.core.Metadata;
import com.gitb.core.ValidationModule;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import com.gitb.vs.*;
import com.gitb.vs.Void;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PluginAdapterTest {

    @Test
    void testGetModuleDefinition() {
        var validator = mock(ValidationService.class);
        when(validator.getModuleDefinition(any())).thenAnswer((Answer<GetModuleDefinitionResponse>) invocation -> {
           var response = new GetModuleDefinitionResponse();
           response.setModule(new ValidationModule());
           response.getModule().setId("id1");
           response.getModule().setMetadata(new Metadata());
           response.getModule().getMetadata().setName("name1");
           return response;
        });
        var adapter = new PluginAdapter(validator, Thread.currentThread().getContextClassLoader());
        var response = adapter.getModuleDefinition(new Void());
        assertNotNull(response);
        assertNotNull(response.getModule());
        assertNotNull(response.getModule().getMetadata());
        assertEquals("id1", response.getModule().getId());
        assertEquals("name1", response.getModule().getMetadata().getName());
        verify(validator, times(1)).getModuleDefinition(any());
    }

    @Test
    void testGetModuleDefinitionNoMethod() {
        var adapter = new PluginAdapter(new Object(), Thread.currentThread().getContextClassLoader());
        assertThrows(IllegalStateException.class, () -> adapter.getModuleDefinition(new Void()));
    }

    @Test
    void testValidate() {
        var expectedRequest = new ValidateRequest();
        expectedRequest.getInput().add(new AnyContent());
        expectedRequest.getInput().get(0).setName("name1");
        expectedRequest.getInput().get(0).setValue("value1");
        expectedRequest.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        expectedRequest.getInput().get(0).setType("string");
        expectedRequest.getInput().add(new AnyContent());
        expectedRequest.getInput().get(1).setName("name2");
        expectedRequest.getInput().get(1).setType("map");
        expectedRequest.getInput().get(1).getItem().add(new AnyContent());
        expectedRequest.getInput().get(1).getItem().get(0).setName("name3");
        expectedRequest.getInput().get(1).getItem().get(0).setValue("value3");
        expectedRequest.getInput().get(1).getItem().get(0).setType("string");
        expectedRequest.getInput().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        var validator = mock(ValidationService.class);
        when(validator.validate(any())).thenAnswer((Answer<ValidationResponse>) invocation -> {
            var objectFactory = new ObjectFactory();
            ValidateRequest receivedRequest = invocation.getArgument(0);
            assertNotNull(receivedRequest);
            assertEquals(expectedRequest.getInput().size(), receivedRequest.getInput().size());
            assertEquals(2, receivedRequest.getInput().size());
            assertEquals(expectedRequest.getInput().get(0).getName(), receivedRequest.getInput().get(0).getName());
            assertEquals(expectedRequest.getInput().get(0).getValue(), receivedRequest.getInput().get(0).getValue());
            assertEquals(expectedRequest.getInput().get(0).getType(), receivedRequest.getInput().get(0).getType());
            assertEquals(expectedRequest.getInput().get(0).getEmbeddingMethod(), receivedRequest.getInput().get(0).getEmbeddingMethod());
            assertEquals(expectedRequest.getInput().get(0).getItem().size(), receivedRequest.getInput().get(0).getItem().size());
            assertEquals(expectedRequest.getInput().get(1).getName(), receivedRequest.getInput().get(1).getName());
            assertEquals(expectedRequest.getInput().get(1).getType(), receivedRequest.getInput().get(1).getType());
            assertEquals(expectedRequest.getInput().get(1).getItem().size(), receivedRequest.getInput().get(1).getItem().size());
            assertEquals(1, receivedRequest.getInput().get(1).getItem().size());
            assertEquals(expectedRequest.getInput().get(1).getItem().get(0).getName(), receivedRequest.getInput().get(1).getItem().get(0).getName());
            assertEquals(expectedRequest.getInput().get(1).getItem().get(0).getValue(), receivedRequest.getInput().get(1).getItem().get(0).getValue());
            assertEquals(expectedRequest.getInput().get(1).getItem().get(0).getType(), receivedRequest.getInput().get(1).getItem().get(0).getType());
            assertEquals(expectedRequest.getInput().get(1).getItem().get(0).getEmbeddingMethod(), receivedRequest.getInput().get(1).getItem().get(0).getEmbeddingMethod());
            var response = new ValidationResponse();
            response.setReport(new TAR());
            response.getReport().setResult(TestResultType.FAILURE);
            response.getReport().setCounters(new ValidationCounters());
            response.getReport().getCounters().setNrOfErrors(BigInteger.ONE);
            response.getReport().getCounters().setNrOfWarnings(BigInteger.ONE);
            response.getReport().getCounters().setNrOfAssertions(BigInteger.ONE);
            response.getReport().setReports(new TestAssertionGroupReportsType());
            response.getReport().getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(new BAR()));
            response.getReport().getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeWarning(new BAR()));
            response.getReport().getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeInfo(new BAR()));
            ((BAR)response.getReport().getReports().getInfoOrWarningOrError().get(0).getValue()).setAssertionID("id1");
            ((BAR)response.getReport().getReports().getInfoOrWarningOrError().get(1).getValue()).setAssertionID("id2");
            ((BAR)response.getReport().getReports().getInfoOrWarningOrError().get(2).getValue()).setAssertionID("id3");
            return response;
        });
        var adapter = new PluginAdapter(validator, Thread.currentThread().getContextClassLoader());
        var response = adapter.validate(expectedRequest);
        assertNotNull(response);
        assertNotNull(response.getReport());
        assertNotNull(response.getReport().getCounters());
        assertNotNull(response.getReport().getReports());
        assertEquals(TestResultType.FAILURE, response.getReport().getResult());
        assertEquals(BigInteger.ONE, response.getReport().getCounters().getNrOfErrors());
        assertEquals(BigInteger.ONE, response.getReport().getCounters().getNrOfWarnings());
        assertEquals(BigInteger.ONE, response.getReport().getCounters().getNrOfAssertions());
        assertEquals(3, response.getReport().getReports().getInfoOrWarningOrError().size());
        assertTrue(response.getReport().getReports().getInfoOrWarningOrError().get(0).getValue() instanceof BAR);
        assertTrue(response.getReport().getReports().getInfoOrWarningOrError().get(1).getValue() instanceof BAR);
        assertTrue(response.getReport().getReports().getInfoOrWarningOrError().get(2).getValue() instanceof BAR);
        assertEquals("id1", ((BAR)response.getReport().getReports().getInfoOrWarningOrError().get(0).getValue()).getAssertionID());
        assertEquals("id2", ((BAR)response.getReport().getReports().getInfoOrWarningOrError().get(1).getValue()).getAssertionID());
        assertEquals("id3", ((BAR)response.getReport().getReports().getInfoOrWarningOrError().get(2).getValue()).getAssertionID());
        verify(validator, times(1)).validate(any());
    }

    @Test
    void testValidateNoMethod() {
        var adapter = new PluginAdapter(new Object(), Thread.currentThread().getContextClassLoader());
        assertThrows(IllegalStateException.class, () -> adapter.validate(new ValidateRequest()));
    }

}
