package eu.europa.ec.itb.validation.commons.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigInteger;
import java.util.GregorianCalendar;

class JsonConfigTest {

    private BAR createBAR() {
        var bar = new BAR();
        bar.setTest("test");
        bar.setDescription("description");
        bar.setType("type");
        bar.setLocation("location");
        bar.setValue("value");
        bar.setAssertionID("assertionID");
        return bar;
    }

    private TAR createTAR() throws DatatypeConfigurationException {
        var objectFactory = new ObjectFactory();
        var report = new TAR();
        report.setResult(TestResultType.FAILURE);
        report.setName("Report");
        report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfAssertions(BigInteger.ONE);
        report.getCounters().setNrOfErrors(BigInteger.ONE);
        report.getCounters().setNrOfWarnings(BigInteger.ONE);
        report.setOverview(new ValidationOverview());
        report.getOverview().setNote("note");
        report.getOverview().setCustomizationID("customisationID");
        report.getOverview().setProfileID("profileID");
        report.getOverview().setTransactionID("transactionID");
        report.getOverview().setValidationServiceName("validationServiceName");
        report.getOverview().setValidationServiceVersion("validationServiceName");
        report.setContext(new AnyContent());
        report.getContext().setType("map");
        report.getContext().getItem().add(new AnyContent());
        report.getContext().getItem().get(0).setType("string");
        report.getContext().getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        report.getContext().getItem().get(0).setValue("value");
        report.getContext().getItem().get(0).setMimeType("text/plain");
        report.getContext().getItem().get(0).setName("name");
        report.getContext().getItem().get(0).setEncoding("utf");
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(createBAR()));
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeWarning(createBAR()));
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeInfo(createBAR()));
        return report;
    }

    @Test
    void testSerialize() throws JsonProcessingException, DatatypeConfigurationException {
        var mapper = JsonConfig.objectMapper();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(createTAR()));
    }

}
