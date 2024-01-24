package eu.europa.ec.itb.validation.commons.report;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.*;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        report.getContext().getItem().get(3).getItem().add(new AnyContent());
        report.getContext().getItem().get(3).getItem().get(1).setName("name6");
        report.getContext().getItem().get(3).getItem().get(1).setType("map");
        report.getContext().getItem().get(3).getItem().get(1).setValue("value6");
        report.getContext().getItem().get(3).getItem().get(1).getItem().add(new AnyContent());
        report.getContext().getItem().get(3).getItem().get(1).getItem().get(0).setName("name7");
        report.getContext().getItem().get(3).getItem().get(1).getItem().get(0).setType("string");
        report.getContext().getItem().get(3).getItem().get(1).getItem().get(0).setValue("value7");
        report.getContext().getItem().get(3).getItem().get(1).getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
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
        assertDoesNotThrow(() -> new ReportGenerator().writeTARReport(createTAR(), Files.newOutputStream(pdfPath), (report) -> mockReportLabels(), false));
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
        assertDoesNotThrow(() -> new ReportGenerator().writeTARReport(Files.newInputStream(xmlPath), Files.newOutputStream(pdfPath), (r) -> mockReportLabels(), false));
        assertTrue(Files.exists(pdfPath));
        assertTrue(Files.isRegularFile(pdfPath));
    }

    private ReportLabels mockReportLabels() {
        var mock = mock(ReportLabels.class);
        when(mock.getAssertionId()).thenReturn("assertionId");
        when(mock.getDate()).thenReturn("date");
        when(mock.getDetails()).thenReturn("details");
        when(mock.getFileName()).thenReturn("fileName");
        when(mock.getFindings()).thenReturn("findings");
        when(mock.getFindingsDetails()).thenReturn("findingsDetails");
        when(mock.getLocation()).thenReturn("location");
        when(mock.getOf()).thenReturn("of");
        when(mock.getOverview()).thenReturn("overview");
        when(mock.getPage()).thenReturn("page");
        when(mock.getResult()).thenReturn("result");
        when(mock.getResultType()).thenReturn("resultType");
        when(mock.getTest()).thenReturn("test");
        when(mock.getTitle()).thenReturn("title");
        return mock;
    }

    private String digest(Path path) throws NoSuchAlgorithmException, IOException {
        var md = MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(path));
        return DatatypeConverter.printHexBinary(md.digest());
    }

    @Test
    void testContextItemNames() throws DatatypeConfigurationException {
        var report = createTAR();
        var reportData = new ReportGenerator().fromTestStepReportType(report, "Title", true);
        assertEquals(5, reportData.getContextItems().size());
        assertEquals("name1", reportData.getContextItems().get(0).getKey());
        assertEquals("name2", reportData.getContextItems().get(1).getKey());
        assertEquals("name3", reportData.getContextItems().get(2).getKey());
        assertEquals("name4.name5", reportData.getContextItems().get(3).getKey());
        assertEquals("name4.name6.name7", reportData.getContextItems().get(4).getKey());
    }

    @Test
    void testEmptyItems() throws DatatypeConfigurationException {
        var report = createTAR();
        report.getReports().getInfoOrWarningOrError().clear();
        var reportData = new ReportGenerator().fromTestStepReportType(report, "Title", true);
        assertNotNull(reportData);
        assertNull(reportData.getReportItems());
    }

    @Test
    void testEmptyContext() throws DatatypeConfigurationException {
        var report = createTAR();
        report.getContext().getItem().clear();
        var reportData = new ReportGenerator().fromTestStepReportType(report, "Title", true);
        assertNotNull(reportData);
        assertNull(reportData.getContextItems());
    }

    @Test
    void testCounters() throws DatatypeConfigurationException {
        var reportWithCounters = createTAR();
        var reportWithoutCounters = createTAR();
        reportWithoutCounters.setCounters(null);
        var reportData1 = new ReportGenerator().fromTestStepReportType(reportWithCounters, "Title", true);
        var reportData2 = new ReportGenerator().fromTestStepReportType(reportWithoutCounters, "Title", true);
        assertNotNull(reportData1);
        assertNotNull(reportData2);
        assertEquals(reportData1.getErrorCount(), reportData2.getErrorCount());
        assertEquals(reportData1.getWarningCount(), reportData2.getWarningCount());
        assertEquals(reportData1.getMessageCount(), reportData2.getMessageCount());
    }

}
