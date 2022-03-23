package eu.europa.ec.itb.validation.commons.report;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.*;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGeneratorTest {

    private Path tmpPath;
    private final ObjectFactory objectFactory = new ObjectFactory();

    @BeforeEach
    void setup() throws IOException {
        tmpPath = Files.createTempDirectory("itb");
    }

    @AfterEach
    void teardown() {
        FileUtils.deleteQuietly(tmpPath.toFile());
    }

    private TAR createTAR() throws DatatypeConfigurationException {
        var report = new TAR();
        report.setResult(TestResultType.FAILURE);
        report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        report.setContext(new AnyContent());
        report.getContext().getItem().add(new AnyContent());
        report.getContext().getItem().get(0).setName("name1");
        report.getContext().getItem().get(0).setValue("value1");
        report.getContext().getItem().get(0).setType("string");
        report.getContext().getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        report.getContext().getItem().add(new AnyContent());
        report.getContext().getItem().get(1).setName("name2");
        report.getContext().getItem().get(1).setValue("http://test.com");
        report.getContext().getItem().get(1).setType("string");
        report.getContext().getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.URI);
        report.getContext().getItem().add(new AnyContent());
        report.getContext().getItem().get(2).setName("name3");
        report.getContext().getItem().get(2).setValue(Base64.getEncoder().encodeToString("value3".getBytes(StandardCharsets.UTF_8)));
        report.getContext().getItem().get(2).setType("string");
        report.getContext().getItem().get(2).setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        report.getContext().getItem().add(new AnyContent());
        report.getContext().getItem().get(3).setName("name4");
        report.getContext().getItem().get(3).setType("map");
        report.getContext().getItem().get(3).getItem().add(new AnyContent());
        report.getContext().getItem().get(3).getItem().get(0).setName("name5");
        report.getContext().getItem().get(3).getItem().get(0).setType("string");
        report.getContext().getItem().get(3).getItem().get(0).setValue("value5");
        report.getContext().getItem().get(3).getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfErrors(BigInteger.ONE);
        report.getCounters().setNrOfWarnings(BigInteger.ONE);
        report.getCounters().setNrOfAssertions(BigInteger.ONE);
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(new BAR()));
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeWarning(new BAR()));
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeInfo(new BAR()));
        setItemContent(report, 0);
        setItemContent(report, 1);
        setItemContent(report, 2);
        return report;
    }

    private void setItemContent(TAR report, int index) {
        ((BAR) report.getReports().getInfoOrWarningOrError().get(index).getValue()).setAssertionID("assertionId"+index);
        ((BAR) report.getReports().getInfoOrWarningOrError().get(index).getValue()).setValue("value"+index);
        ((BAR) report.getReports().getInfoOrWarningOrError().get(index).getValue()).setDescription("description"+index);
        ((BAR) report.getReports().getInfoOrWarningOrError().get(index).getValue()).setType("type"+index);
        ((BAR) report.getReports().getInfoOrWarningOrError().get(index).getValue()).setTest("test"+index);
        ((BAR) report.getReports().getInfoOrWarningOrError().get(index).getValue()).setLocation("0:0");
    }

    @Test
    void testWriteTARReportFromObject() {
        var pdfPath = Path.of(tmpPath.toString(), "report.pdf");
        assertDoesNotThrow(() -> new ReportGenerator().writeTARReport(createTAR(), Files.newOutputStream(pdfPath), (report) -> new ReportLabels()));
        assertTrue(Files.exists(pdfPath));
        assertTrue(Files.isRegularFile(pdfPath));
    }

    @Test
    void testWriteTARReportFromXML() throws JAXBException, DatatypeConfigurationException, IOException {
        var report = createTAR();
        var xmlPath = Path.of(tmpPath.toString(), "report.xml");
        var jaxbContext = JAXBContext.newInstance(TAR.class, TestCaseReportType.class, TestStepStatus.class);
        jaxbContext.createMarshaller().marshal(new JAXBElement<>(new QName("http://www.gitb.com/tr/v1/", "TestCaseReport"), TAR.class, report), Files.newOutputStream(xmlPath));
        var pdfPath = Path.of(tmpPath.toString(), "report.pdf");
        assertDoesNotThrow(() -> new ReportGenerator().writeTARReport(Files.newInputStream(xmlPath), Files.newOutputStream(pdfPath), (r) -> new ReportLabels()));
        assertTrue(Files.exists(pdfPath));
        assertTrue(Files.isRegularFile(pdfPath));
    }

}
