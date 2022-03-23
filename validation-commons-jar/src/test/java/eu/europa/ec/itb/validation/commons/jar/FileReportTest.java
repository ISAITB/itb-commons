package eu.europa.ec.itb.validation.commons.jar;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigInteger;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileReportTest {

    @Test
    void testWithType() throws DatatypeConfigurationException {
        var fileName = "name1.xml";
        var report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setCounters(new ValidationCounters());
        report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        report.getCounters().setNrOfErrors(BigInteger.ZERO);
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        var fileReport = new FileReport(fileName, report, true, "type1");
        var text = fileReport.toString();
        assertTrue(text.contains("Validation report summary ["+fileName+"]"));
        assertTrue(text.contains("Validation type: type1"));
        assertTrue(text.contains("Date: "+report.getDate()));
        assertTrue(text.contains("Result: "+report.getResult()));
        assertTrue(text.contains("Errors: 0"));
        assertTrue(text.contains("Warnings: 0"));
        assertTrue(text.contains("Messages: 0"));
    }

    @Test
    void testWithoutType() throws DatatypeConfigurationException {
        var fileName = "name1.xml";
        var report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setCounters(new ValidationCounters());
        report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        report.getCounters().setNrOfErrors(BigInteger.ZERO);
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        var fileReport = new FileReport(fileName, report);
        var text = fileReport.toString();
        assertTrue(text.contains("Validation report summary ["+fileName+"]"));
        assertFalse(text.contains("Validation type:"));
        assertTrue(text.contains("Date: "+report.getDate()));
        assertTrue(text.contains("Result: "+report.getResult()));
        assertTrue(text.contains("Errors: 0"));
        assertTrue(text.contains("Warnings: 0"));
        assertTrue(text.contains("Messages: 0"));
    }

}
