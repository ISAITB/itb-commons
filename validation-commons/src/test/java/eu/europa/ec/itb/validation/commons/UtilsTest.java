package eu.europa.ec.itb.validation.commons;

import com.gitb.core.AnyContent;
import com.gitb.core.ConfigurationType;
import com.gitb.core.UsageEnumeration;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import org.apache.commons.lang3.LocaleUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilsTest extends BaseTest {

    @Test
    void testGetXMLGregorianCalendarDateTime() {
        var result = Utils.getXMLGregorianCalendarDateTime();
        assertNotNull(result, "Calendar was null.");
        var now = Calendar.getInstance();
        assertEquals(now.get(Calendar.DAY_OF_MONTH), result.getDay(), "Day of month does not match.");
        assertEquals(now.get(Calendar.MONTH), result.getMonth()-1, "Month does not match.");
        assertEquals(now.get(Calendar.YEAR), result.getYear(), "Year does not match.");
        assertEquals(now.get(Calendar.HOUR_OF_DAY), result.getHour(), "Hour does not match.");
        assertEquals(now.get(Calendar.MINUTE), result.getMinute(), "Minute does not match.");
    }

    @Test
    void testSerializeExact() throws ParserConfigurationException, IOException, SAXException {
        var xmlString = "<test>Test content</test>";
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        var bytes = Utils.serialize(document);
        assertNotNull(bytes, "Bytes were null.");
        var resultString = new String(bytes);
        assertEquals(xmlString, resultString.trim(), "Result did not match input.");
    }

    @Test
    void testSerializeTransformed() throws ParserConfigurationException, IOException, SAXException {
        var xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><parent><child>Child</child></parent>";
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        var bytes = Utils.serialize(document);
        assertNotNull(bytes, "Bytes were null.");
        var resultString = new String(bytes);
        var p = Pattern.compile("^<parent>[\\n\\r]+[ \\t]+<child>Child<\\/child>[\\n\\r]+<\\/parent>[\\n\\r]?$");
        assertTrue(p.matcher(resultString).lookingAt(), "Unexpected serialisation output");
    }

    @Test
    void testEmptyDocument() throws TransformerException {
        var document = Utils.emptyDocument();
        var result = new StreamResult(new StringWriter());
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(document), result);
        assertEquals("", result.getWriter().toString(), "Document should have been empty.");
    }

    @Test
    void testReadXMLWithLineNumber() throws IOException, SAXException {
        String xml =
            "<parent>\n"+
                "<child>Child1</child>\n"+
                "<child>Child2</child>\n"+
            "</parent>";
        var result = Utils.readXMLWithLineNumbers(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(result);
        var childNodes = result.getDocumentElement().getElementsByTagName("child");
        assertEquals(2, childNodes.getLength());
        var child1 = childNodes.item(0);
        assertEquals("Child1", child1.getTextContent());
        assertNotNull(child1.getUserData(Utils.LINE_NUMBER_KEY_NAME));
        assertInstanceOf(String.class, child1.getUserData(Utils.LINE_NUMBER_KEY_NAME));
        assertEquals(2, Integer.parseInt((String) child1.getUserData(Utils.LINE_NUMBER_KEY_NAME)));
        var child2 = childNodes.item(1);
        assertEquals("Child2", child2.getTextContent());
        assertNotNull(child2.getUserData(Utils.LINE_NUMBER_KEY_NAME));
        assertInstanceOf(String.class, child2.getUserData(Utils.LINE_NUMBER_KEY_NAME));
        assertEquals(3, Integer.parseInt((String) child2.getUserData(Utils.LINE_NUMBER_KEY_NAME)));
    }

    @Test
    void testCreateParameter() {
        var result = Utils.createParameter("name", "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "description");
        assertEquals("name", result.getName());
        assertEquals("string", result.getType());
        assertEquals(UsageEnumeration.R, result.getUse());
        assertEquals(ConfigurationType.SIMPLE, result.getKind());
        assertEquals("description", result.getDesc());
    }

    @Test
    void testCreateSimpleParameter() {
        var result = Utils.createParameter("name", "string", UsageEnumeration.R, "description");
        assertEquals("name", result.getName());
        assertEquals("string", result.getType());
        assertEquals(UsageEnumeration.R, result.getUse());
        assertEquals(ConfigurationType.SIMPLE, result.getKind());
        assertEquals("description", result.getDesc());
    }

    private AnyContent createAnyContent(int index) {
        var anyContent = new AnyContent();
        anyContent.setName("name_"+index);
        anyContent.setType("string");
        anyContent.setValue("value_"+index);
        return anyContent;
    }

    private List<AnyContent> createAnyContentList(int size) {
        var result = new ArrayList<AnyContent>(size);
        for (int i=1; i <= size; i++) {
            result.add(createAnyContent(i));
        }
        return result;
    }

    @Test
    void testGetInputForValidateRequest() {
        var request = new ValidateRequest();
        request.getInput().addAll(createAnyContentList(3));
        var result = Utils.getInputFor(request, "name_X");
        assertEquals(0, result.size());
        result = Utils.getInputFor(request, "name_2");
        assertEquals(1, result.size());
        assertEquals("value_2", result.get(0).getValue());
    }

    @Test
    void testGetInputForList() {
        var list = createAnyContentList(3);
        var result = Utils.getInputFor(list, "name_X");
        assertEquals(0, result.size());
        result = Utils.getInputFor(list, "name_2");
        assertEquals(1, result.size());
        assertEquals("value_2", result.get(0).getValue());
    }

    private BAR createBAR(int index) {
        var bar = new BAR();
        bar.setAssertionID("ID_"+index);
        return bar;
    }

    private List<TAR> createReportsToTest() {
        ObjectFactory objectFactory = new ObjectFactory();
        // Report 1.
        TAR report1 = objectFactory.createTAR();
        report1.setCounters(new ValidationCounters());
        report1.getCounters().setNrOfAssertions(BigInteger.ZERO);
        report1.getCounters().setNrOfWarnings(BigInteger.ONE);
        report1.getCounters().setNrOfErrors(BigInteger.TWO);
        report1.setResult(TestResultType.FAILURE);
        report1.setReports(new TestAssertionGroupReportsType());
        report1.getReports().getInfoOrWarningOrError().addAll(List.of(
                objectFactory.createTestAssertionGroupReportsTypeError(createBAR(1)),
                objectFactory.createTestAssertionGroupReportsTypeError(createBAR(2)),
                objectFactory.createTestAssertionGroupReportsTypeWarning(createBAR(3))
        ));
        report1.setContext(new AnyContent());
        report1.getContext().getItem().add(new AnyContent());
        report1.getContext().getItem().get(0).setName("name1");
        report1.getContext().getItem().get(0).setValue("value1");
        report1.getContext().getItem().get(0).setType("string");
        report1.getContext().getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        // Report 2.
        TAR report2 = objectFactory.createTAR();
        report2.setCounters(new ValidationCounters());
        report2.getCounters().setNrOfAssertions(BigInteger.ONE);
        report2.getCounters().setNrOfWarnings(BigInteger.ONE);
        report2.getCounters().setNrOfErrors(BigInteger.ZERO);
        report2.setResult(TestResultType.WARNING);
        report2.setReports(new TestAssertionGroupReportsType());
        report2.getReports().getInfoOrWarningOrError().addAll(List.of(
                objectFactory.createTestAssertionGroupReportsTypeInfo(createBAR(4)),
                objectFactory.createTestAssertionGroupReportsTypeWarning(createBAR(5))
        ));
        report2.setContext(new AnyContent());
        report2.getContext().getItem().add(new AnyContent());
        report2.getContext().getItem().get(0).setName("name2");
        report2.getContext().getItem().get(0).setValue("value2");
        report2.getContext().getItem().get(0).setType("string");
        report2.getContext().getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        return List.of(report1, report2);
    }

    private void validateMergedReport(TAR result) {
        assertNotNull(result);
        assertEquals(TestResultType.FAILURE, result.getResult());
        assertNotNull(result.getCounters());
        assertEquals(BigInteger.ONE, result.getCounters().getNrOfAssertions());
        assertEquals(BigInteger.TWO, result.getCounters().getNrOfWarnings());
        assertEquals(BigInteger.TWO, result.getCounters().getNrOfErrors());
        assertNotNull(result.getReports());
        assertEquals(5, result.getReports().getInfoOrWarningOrError().size());
        assertNotNull(result.getContext());
        assertEquals(2, result.getContext().getItem().size());
        result.getContext().getItem().sort(Comparator.comparing(AnyContent::getName));
        assertEquals("name1", result.getContext().getItem().get(0).getName());
        assertEquals("value1", result.getContext().getItem().get(0).getValue());
        assertEquals("string", result.getContext().getItem().get(0).getType());
        assertEquals(ValueEmbeddingEnumeration.STRING, result.getContext().getItem().get(0).getEmbeddingMethod());
        assertEquals("name2", result.getContext().getItem().get(1).getName());
        assertEquals("value2", result.getContext().getItem().get(1).getValue());
        assertEquals("string", result.getContext().getItem().get(1).getType());
        assertEquals(ValueEmbeddingEnumeration.STRING, result.getContext().getItem().get(1).getEmbeddingMethod());
    }

    @Test
    void testMergeReportsAsList() {
        validateMergedReport(Utils.mergeReports(createReportsToTest()));
    }

    @Test
    void testMergeReportsAsListEmpty() {
        var report1 = new TAR();
        var report2 = new TAR();
        report2.setCounters(new ValidationCounters());
        report2.setReports(new TestAssertionGroupReportsType());
        report2.setContext(new AnyContent());
        var result = Utils.mergeReports(List.of(report1, report2));
        assertNotNull(result);
        assertNotNull(result.getCounters());
        assertEquals(BigInteger.ZERO, result.getCounters().getNrOfErrors());
        assertEquals(BigInteger.ZERO, result.getCounters().getNrOfWarnings());
        assertEquals(BigInteger.ZERO, result.getCounters().getNrOfAssertions());
        assertNotNull(result.getReports());
        assertEquals(0, result.getReports().getInfoOrWarningOrError().size());
        assertNotNull(result.getContext());
    }

    @Test
    void testMergeReportsAsArray() {
        validateMergedReport(Utils.mergeReports(createReportsToTest().toArray(new TAR[0])));
    }

    private void validateCreatedInputItem(AnyContent result, String encoding) {
        assertNotNull(result);
        assertEquals("name", result.getName());
        assertEquals("value", result.getValue());
        assertEquals(ValueEmbeddingEnumeration.STRING, result.getEmbeddingMethod());
        assertEquals("string", result.getType());
        if (encoding == null) {
            assertNull(result.getEncoding());
        } else {
            assertEquals(encoding, result.getEncoding());
        }
    }

    @Test
    void testCreateInputItem() {
        validateCreatedInputItem(Utils.createInputItem("name", "value"), null);
        validateCreatedInputItem(Utils.createInputItem("name", "value", ValueEmbeddingEnumeration.STRING), null);
        validateCreatedInputItem(Utils.createInputItem("name", "value", ValueEmbeddingEnumeration.STRING, "string"), null);
        validateCreatedInputItem(Utils.createInputItem("name", "value", ValueEmbeddingEnumeration.STRING, "string", "UTF-8"), "UTF-8");
    }

    @Test
    void testGetSupportedLocale() {
        var domain = mock(DomainConfig.class);
        when(domain.getDefaultLocale()).thenReturn(Locale.GERMAN);
        when(domain.getAvailableLocales()).thenReturn(Set.of(Locale.GERMAN, Locale.FRENCH));
        assertEquals(Locale.FRENCH, Utils.getSupportedLocale(Locale.FRENCH, domain));
        assertEquals(Locale.GERMAN, Utils.getSupportedLocale(Locale.ITALIAN, domain));
        assertEquals(Locale.GERMAN, Utils.getSupportedLocale(null, domain));
        assertEquals(Locale.GERMAN, Utils.getSupportedLocale(LocaleUtils.toLocale(""), domain));
    }

    @Test
    void testToTARWithFileError() {
        var tmpFolderAsFile = tmpFolder.toFile();
        assertThrows(IllegalStateException.class, () -> Utils.toTAR(tmpFolderAsFile));
    }

    @Test
    void testToTARWithFile() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFile.xml")), tarFile);
        var result = Utils.toTAR(tarFile.toFile());
        assertNotNull(result);
        assertNotNull(result.getReports());
        assertNotNull(result.getReports().getInfoOrWarningOrError());
        assertEquals(5, result.getReports().getInfoOrWarningOrError().size());
        assertEquals(TestResultType.FAILURE, result.getResult());
    }

    @Test
    void testToTARWithStreamError() {
        assertThrows(IllegalStateException.class, () -> Utils.toTAR((InputStream) null));
    }

    @Test
    void testToTARWithStream() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFile.xml")), tarFile);
        var result = Utils.toTAR(Files.newInputStream(tarFile));
        assertNotNull(result);
        assertNotNull(result.getReports());
        assertNotNull(result.getReports().getInfoOrWarningOrError());
        assertEquals(5, result.getReports().getInfoOrWarningOrError().size());
        assertEquals(TestResultType.FAILURE, result.getResult());
    }

    @Test
    void testToAggregateTAR() throws IOException {
        var tarFile = Path.of(tmpFolder.toString(), "tarFileForAggregate.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFileForAggregate.xml")), tarFile);
        var detailed = Utils.toTAR(tarFile.toFile());
        detailed.setContext(new AnyContent());
        detailed.getContext().setValue("Value for context");
        // Sanity check for test data
        assertEquals(5, detailed.getReports().getInfoOrWarningOrError().size());
        var aggregated = Utils.toAggregatedTAR(detailed, getLocalisationHelper());
        assertNotSame(detailed, aggregated);
        assertEquals(detailed.getResult(), aggregated.getResult());
        assertEquals(detailed.getDate(), aggregated.getDate());
        assertNotNull(aggregated.getContext());
        assertNull(aggregated.getContext().getValue());
        assertEquals(detailed.getCounters().getNrOfAssertions(), aggregated.getCounters().getNrOfAssertions());
        assertEquals(detailed.getCounters().getNrOfErrors(), aggregated.getCounters().getNrOfErrors());
        assertEquals(detailed.getCounters().getNrOfWarnings(), aggregated.getCounters().getNrOfWarnings());
        assertEquals(3, aggregated.getReports().getInfoOrWarningOrError().size());
        assertEquals("[Total 3] description1", ((BAR)aggregated.getReports().getInfoOrWarningOrError().get(0).getValue()).getDescription());
        assertEquals("description4", ((BAR)aggregated.getReports().getInfoOrWarningOrError().get(1).getValue()).getDescription());
        assertEquals("description5", ((BAR)aggregated.getReports().getInfoOrWarningOrError().get(2).getValue()).getDescription());
    }

    @Test
    void testAggregateDiffers() throws IOException {
        var tarFile1 = Path.of(tmpFolder.toString(), "tarFileForAggregate.xml");
        var tarFile2 = Path.of(tmpFolder.toString(), "tarFile.xml");
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFileForAggregate.xml")), tarFile1);
        Files.copy(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/tarFile.xml")), tarFile2);
        var detailed1 = Utils.toTAR(tarFile1.toFile());
        var aggregated1 = Utils.toAggregatedTAR(detailed1, getLocalisationHelper());
        var detailed2 = Utils.toTAR(tarFile2.toFile());
        var aggregated2 = Utils.toAggregatedTAR(detailed2, getLocalisationHelper());
        assertTrue(Utils.aggregateDiffers(detailed1, aggregated1));
        assertFalse(Utils.aggregateDiffers(detailed2, aggregated2));
        assertFalse(Utils.aggregateDiffers(detailed1, null));
        assertFalse(Utils.aggregateDiffers(null, aggregated1));
        assertFalse(Utils.aggregateDiffers(null, null));
    }

    private LocalisationHelper getLocalisationHelper() {
        var localiser = mock(LocalisationHelper.class);
        when(localiser.localise(eq("validator.label.reportItemTotalOccurrences"), any())).thenAnswer((a) -> {
            assertEquals(2, a.getArguments().length);
            assertInstanceOf(String.class, a.getArgument(0));
            assertInstanceOf(Long.class, a.getArgument(1));
            return "Total "+ a.getArgument(1);
        });
        return localiser;
    }

    private BAR createBAR() {
        return createBAR("description");
    }

    private BAR createBAR(String description) {
        var bar = new BAR();
        bar.setTest("test");
        bar.setDescription(description);
        bar.setType("type");
        bar.setLocation("location");
        bar.setValue("value");
        bar.setAssertionID("assertionID");
        return bar;
    }

    @Test
    void testLimitReportItemsIfNeeded() {
        var report = new TAR();
        var objectFactory = new ObjectFactory();
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(createBAR()));
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeWarning(createBAR()));
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeInfo(createBAR()));

        var config = mock(DomainConfig.class);
        when(config.getMaximumReportsForXmlOutput()).thenReturn(5L);
        Utils.limitReportItemsIfNeeded(report, config);
        assertEquals(3, report.getReports().getInfoOrWarningOrError().size());

        when(config.getMaximumReportsForXmlOutput()).thenReturn(1L);
        Utils.limitReportItemsIfNeeded(report, config);
        assertEquals(1, report.getReports().getInfoOrWarningOrError().size());
    }

    @Test
    void testSecureSchemaFactory() {
        var result = Utils.secureSchemaFactory();
        assertNotNull(result);
    }

    @Test
    void testSecureXMLInputFactory() {
        var result = Utils.secureXMLInputFactory();
        assertNotNull(result);
        assertInstanceOf(Boolean.class, result.getProperty(XMLInputFactory.SUPPORT_DTD));
        assertFalse((Boolean) result.getProperty(XMLInputFactory.SUPPORT_DTD));
    }

    @Test
    void testSanitizeInactive() {
        var report = new TAR();
        var objectFactory = new ObjectFactory();
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(createBAR("<a href=\"something\">TEXT</a> <b>BOLD</b>")));
        var config = mock(DomainConfig.class);
        when(config.isRichTextReports()).thenReturn(false);

        Utils.sanitizeIfNeeded(report, config);
        assertEquals("<a href=\"something\">TEXT</a> <b>BOLD</b>", ((BAR)report.getReports().getInfoOrWarningOrError().get(0).getValue()).getDescription());
    }

    @Test
    void testSanitizeActive() {
        var report = new TAR();
        var objectFactory = new ObjectFactory();
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(createBAR("<a href=\"something\" target=\"_self\">TEXT</a> <b>BOLD</b>")));
        var config = mock(DomainConfig.class);
        when(config.isRichTextReports()).thenReturn(true);

        Utils.sanitizeIfNeeded(report, config);
        assertEquals("<a href=\"something\">TEXT</a> BOLD", ((BAR)report.getReports().getInfoOrWarningOrError().get(0).getValue()).getDescription());
    }
}
