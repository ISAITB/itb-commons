package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestAssertionReportType;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;

import static eu.europa.ec.itb.validation.commons.ReportItemComparatorTest.Severity.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReportItemComparatorTest {

    ObjectFactory objectFactory = new ObjectFactory();

    @Test
    void testComparison() {
        var elements = new ArrayList<JAXBElement<TestAssertionReportType>>();
        elements.add(createBAR("Error 1", ERROR));
        elements.add(createBAR("Error 2", MESSAGE));
        elements.add(null);
        elements.add(null);
        elements.add(createBAR("Error 3", MESSAGE));
        elements.add(createBAR("Error 4", WARNING));
        elements.add(createBAR("Error 5", ERROR));
        elements.add(createBAR("Error 6", MESSAGE));
        elements.add(createBAR("Error 7", WARNING));
        elements.add(createBAR("Error 8", MESSAGE));
        elements.add(createBAR("Error 9", OTHER));
        elements.sort(new ReportItemComparator());
        int i = 0;
        assertNull(elements.get(i++));
        assertNull(elements.get(i++));
        assertEquals("Error 1", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 5", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 4", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 7", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 2", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 3", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 6", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 8", ((BAR)elements.get(i++).getValue()).getDescription());
        assertEquals("Error 9", ((BAR)elements.get(i).getValue()).getDescription());
    }

    private JAXBElement<TestAssertionReportType> createBAR(String description, ReportItemComparatorTest.Severity severity) {
        var bar = new BAR();
        bar.setDescription(description);
        switch (severity) {
            case ERROR: return objectFactory.createTestAssertionGroupReportsTypeError(bar);
            case WARNING: return objectFactory.createTestAssertionGroupReportsTypeWarning(bar);
            case OTHER: return new JAXBElement<>(new QName("http://www.gitb.com/tr/v1/", "other"), TestAssertionReportType.class, TestAssertionGroupReportsType.class, bar);
            default:  return objectFactory.createTestAssertionGroupReportsTypeInfo(bar);
        }
    }

    enum Severity {
        ERROR, WARNING,MESSAGE,OTHER;
    }

}
