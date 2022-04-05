package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TestAssertionReportType;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AggregateReportItemsTest {

    ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void testUpdateWithDefaultClassifierFunction() {
        LocalisationHelper localiser = getLocalisationHelper();
        List<JAXBElement<TestAssertionReportType>> elements = getReportElements();
        var reportItems = new AggregateReportItems(objectFactory, localiser);
        for (var element: elements) {
            reportItems.updateForReportItem(element);
        }
        var result = reportItems.getReportItems();
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("[Total 3] Error 1", ((BAR)result.get(0).getValue()).getDescription());
        assertEquals("location 1", ((BAR)result.get(0).getValue()).getLocation());
        assertEquals("error", result.get(0).getName().getLocalPart());
        assertEquals("Error 2", ((BAR)result.get(1).getValue()).getDescription());
        assertEquals("location 2", ((BAR)result.get(1).getValue()).getLocation());
        assertEquals("error", result.get(1).getName().getLocalPart());
        assertEquals("Error 2", ((BAR)result.get(2).getValue()).getDescription());
        assertEquals("location 5", ((BAR)result.get(2).getValue()).getLocation());
        assertEquals("warning", result.get(2).getName().getLocalPart());
        assertEquals("Error 3", ((BAR)result.get(3).getValue()).getDescription());
        assertEquals("location 6", ((BAR)result.get(3).getValue()).getLocation());
        assertEquals("info", result.get(3).getName().getLocalPart());
    }

    @Test
    void testUpdateWithCustomClassifierFunction() {
        LocalisationHelper localiser = getLocalisationHelper();
        List<JAXBElement<TestAssertionReportType>> elements = getReportElements();
        var reportItems = new AggregateReportItems(objectFactory, localiser);
        for (var element: elements) {
            reportItems.updateForReportItem(element, (e) -> "Same string");
        }
        var result = reportItems.getReportItems();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("[Total "+elements.size()+"] Error 1", ((BAR)result.get(0).getValue()).getDescription());
    }

    @Test
    void testBadElementType() {
        var reportItems = new AggregateReportItems(objectFactory, mock(LocalisationHelper.class));
        var badElement = objectFactory.createTestAssertionGroupReportsTypeWarning(new TestAssertionReportType() {});
        assertThrows(IllegalStateException.class, () -> reportItems.updateForReportItem(badElement));
    }

    private List<JAXBElement<TestAssertionReportType>> getReportElements() {
        return List.of(
                createBAR("Error 1", "location 1", Severity.ERROR),   // 1
                createBAR("Error 2", "location 2", Severity.ERROR),   // 2
                createBAR("Error 1", "location 3", Severity.ERROR),   // 1
                createBAR("Error 1", "location 4", Severity.ERROR),   // 1
                createBAR("Error 2", "location 5", Severity.WARNING), // 3
                createBAR("Error 3", "location 6", Severity.MESSAGE)  // 4
        );
    }

    private LocalisationHelper getLocalisationHelper() {
        var localiser = mock(LocalisationHelper.class);
        when(localiser.localise(eq("validator.label.reportItemTotalOccurrences"), any())).thenAnswer((a) -> {
            assertEquals(2, a.getArguments().length);
            assertTrue(a.getArgument(0) instanceof String);
            assertTrue(a.getArgument(1) instanceof Long);
            return "Total "+ a.getArgument(1);
        });
        return localiser;
    }

    private JAXBElement<TestAssertionReportType> createBAR(String description, String location, Severity severity) {
        var bar = new BAR();
        bar.setDescription(description);
        bar.setLocation(location);
        switch (severity) {
            case ERROR: return objectFactory.createTestAssertionGroupReportsTypeError(bar);
            case WARNING: return objectFactory.createTestAssertionGroupReportsTypeWarning(bar);
            default:  return objectFactory.createTestAssertionGroupReportsTypeInfo(bar);
        }
    }

    enum Severity {
        ERROR, WARNING,MESSAGE
    }

}
