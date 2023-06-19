package eu.europa.ec.itb.validation.commons;

import com.gitb.core.*;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Class holding utility methods for common operations.
 */
public class Utils {

    private static final JAXBContext tdlJaxbContext;

    static {
        try {
            tdlJaxbContext = JAXBContext.newInstance(
                    TAR.class,
                    TestCaseReportType.class,
                    TestStepStatus.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to initialise JAXB context for TDL classes", e);
        }
    }

    /**
     * Constructor to prevent instantiation.
     */
    private Utils() { throw new IllegalStateException("Utility class"); }

    /**
     * A key used to record line numbers in parsed XML content.
     */
    public static final String LINE_NUMBER_KEY_NAME = "lineNumber";

    /**
     * Create a calendar for the current time.
     *
     * @return The calendar.
     */
    public static XMLGregorianCalendar getXMLGregorianCalendarDateTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Unable to construct data type factory for date", e);
        }
    }

    /**
     * Create a secured XML transformer instance.
     *
     * @return The transformer to use.
     */
    public static Transformer secureTransformer() {
        try {
            return secureTransformerFactory().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("Could not create XML transformer", e);
        }
    }

    /**
     * Create a secured XML transformer factory instance.
     *
     * @return The factory to use.
     */
    public static TransformerFactory secureTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }

    /**
     * Create a secured XML Document Builder instance.
     *
     * @return The document builder to use.
     */
    public static DocumentBuilder secureDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setNamespaceAware(true);
            dbf.setXIncludeAware(false);
            dbf.setValidating(false);
            dbf.setExpandEntityReferences(false);
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Could not create XML document builder", e);
        }
    }

    /**
     * Create a secured XML SAX parser instance.
     *
     * @return The SAX parser to use.
     */
    public static SAXParser secureSAXParser() {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setXIncludeAware(false);
            spf.setValidating(false);
            return spf.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Could not create XML SAX parser", e);
        }
    }

    /**
     * Serialise the given XML source to the provided output stream.
     *
     * @param content The source.
     * @param outputStream The target output to write to.
     */
    public static void serialize(Source content, OutputStream outputStream) {
        try {
            Transformer transformer = secureTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.transform(content, new StreamResult(outputStream));
        } catch (TransformerException te) {
            throw new IllegalStateException(te);
        }
    }

    /**
     * Serialise the provided node to a byte array.
     *
     * @param content The content.
     * @return the node's bytes.
     */
    public static byte[] serialize(Node content) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serialize(new DOMSource(content), output);
        return output.toByteArray();
    }

    /**
     * Create an empty XML DOM document.
     *
     * @return The document.
     */
    public static Document emptyDocument() {
        return secureDocumentBuilder().newDocument();
    }

    /**
     * Read an XML file from the provided stream and record on each node the its line number for
     * subsequent user as user data.
     *
     * @param is The input stream.
     * @return The parsed document. The nodes of this each define user data with their relevant line number.
     * @throws IOException If a general processing error occurs.
     * @throws SAXException If an error occurs when parsing the XML.
     */
    public static Document readXMLWithLineNumbers(InputStream is) throws IOException, SAXException {
        final Document doc = secureDocumentBuilder().newDocument();
        final SAXParser parser = secureSAXParser();

        final Deque<Element> elementStack = new LinkedList<>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            /**
             * @see DefaultHandler#setDocumentLocator(Locator)
             */
            @Override
            public void setDocumentLocator(final Locator locator) {
                this.locator = locator; // Save the locator, so that it can be used later for line tracking when traversing nodes.
            }

            /**
             * Adds the line number as node user data.
             *
             * @see DefaultHandler#startElement(String, String, String, Attributes)
             */
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                addTextIfNeeded();
                final org.w3c.dom.Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }
                el.setUserData(LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            /**
             * @see DefaultHandler#endElement(String, String, String)
             */
            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                addTextIfNeeded();
                final org.w3c.dom.Element closedEl = elementStack.pop();
                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    final org.w3c.dom.Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            /**
             * @see DefaultHandler#characters(char[], int, int)
             */
            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                textBuffer.append(ch, start, length);
            }

            /**
             * Add text under the current node.
             */
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    final org.w3c.dom.Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };
        parser.parse(is, handler);
        return doc;
    }

    /**
     * Create a parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param kind The kind of parameter it is (whether it should be provided as the specific value, as BASE64 content or as a URL that needs to be looked up to obtain the value).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    public static TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    /**
     * Create a simple parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    public static TypedParameter createParameter(String name, String type, UsageEnumeration use, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(ConfigurationType.SIMPLE);
        parameter.setDesc(description);
        return parameter;
    }

    /**
     * Get the input(s) for the provided name from the provided validation parameters.
     *
     * @param validateRequest The request parameters to look into.
     * @param name The input name to look for.
     * @return The list of matched inputs (never null).
     */
    public static List<AnyContent> getInputFor(ValidateRequest validateRequest, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (validateRequest != null && validateRequest.getInput() != null) {
            inputs.addAll(getInputFor(validateRequest.getInput(), name));
        }
        return inputs;
    }

    /**
     * Get the input(s) for the provided name from the provided validation parameters.
     *
     * @param inputsToConsider The inputs to look into.
     * @param name The input name to look for.
     * @return The list of matched inputs (never null).
     */
    public static List<AnyContent> getInputFor(List<AnyContent> inputsToConsider, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (inputsToConsider != null) {
            for (AnyContent anInput: inputsToConsider) {
                if (name.equals(anInput.getName())) {
                    inputs.add(anInput);
                }
            }
        }
        return inputs;
    }

    /**
     * Merge the provided TAR reports into a single one.
     *
     * @param reports The reports to merge.
     * @return The merged report.
     */
    public static TAR mergeReports(List<TAR> reports) {
        return mergeReports(reports.toArray(new TAR[0]));
    }

    /**
     * Merge the provided TAR reports into a single one.
     *
     * @param reports The reports to merge.
     * @return The merged report.
     */
    public static TAR mergeReports(TAR[] reports) {
        TAR mergedReport = reports[0];
        if (reports.length > 1) {
            for (int i=1; i < reports.length; i++) {
                TAR report = reports[i];
                if (report != null) {
                    if (report.getCounters() != null) {
                        if (mergedReport.getCounters() == null) {
                            mergedReport.setCounters(new ValidationCounters());
                            mergedReport.getCounters().setNrOfAssertions(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfWarnings(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfErrors(BigInteger.ZERO);
                        }
                        if (report.getCounters().getNrOfAssertions() != null) {
                            mergedReport.getCounters().setNrOfAssertions(mergedReport.getCounters().getNrOfAssertions().add(report.getCounters().getNrOfAssertions()));
                        }
                        if (report.getCounters().getNrOfWarnings() != null) {
                            mergedReport.getCounters().setNrOfWarnings(mergedReport.getCounters().getNrOfWarnings().add(report.getCounters().getNrOfWarnings()));
                        }
                        if (report.getCounters().getNrOfErrors() != null) {
                            mergedReport.getCounters().setNrOfErrors(mergedReport.getCounters().getNrOfErrors().add(report.getCounters().getNrOfErrors()));
                        }
                    }
                    if (report.getReports() != null) {
                        if (mergedReport.getReports() == null) {
                            mergedReport.setReports(new TestAssertionGroupReportsType());
                        }
                        mergedReport.getReports().getInfoOrWarningOrError().addAll(report.getReports().getInfoOrWarningOrError());
                    }
                    if (mergedReport.getResult() == null) {
                        mergedReport.setResult(TestResultType.UNDEFINED);
                    }
                    if (report.getResult() != null && report.getResult() != TestResultType.UNDEFINED &&
                            ((mergedReport.getResult() == TestResultType.UNDEFINED) ||
                                (mergedReport.getResult() == TestResultType.SUCCESS && report.getResult() != TestResultType.SUCCESS) ||
                                (mergedReport.getResult() == TestResultType.WARNING && report.getResult() == TestResultType.FAILURE))) {
                        mergedReport.setResult(report.getResult());
                    }
                    if (report.getContext() != null) {
                        if (mergedReport.getContext() == null) {
                            mergedReport.setContext(report.getContext());
                        } else {
                            if (report.getContext().getItem() != null) {
                                for (AnyContent item: report.getContext().getItem()) {
                                    if (item.getName() != null) {
                                        List<AnyContent> matchedInputs = getInputFor(mergedReport.getContext().getItem(), item.getName());
                                        if (matchedInputs.isEmpty()) {
                                            mergedReport.getContext().getItem().add(item);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return mergedReport;
    }

    /**
     * Create an input item of type string for the provided information.
     *
     * @param name The input's name.
     * @param value The input's value considered as a string.
     * @return The input.
     */
    public static AnyContent createInputItem(String name, String value) {
        return createInputItem(name, value, ValueEmbeddingEnumeration.STRING);
    }

    /**
     * Create an input item of type string for the provided information.
     *
     * @param name The input's name.
     * @param value The input's value.
     * @param embeddingType The embedding method to use for the input's content.
     * @return The input.
     */
    public static AnyContent createInputItem(String name, String value, ValueEmbeddingEnumeration embeddingType) {
        return createInputItem(name, value, embeddingType, "string");
    }

    /**
     * Create an input item of for the provided information.
     *
     * @param name The input's name.
     * @param value The input's value.
     * @param embeddingType The embedding method to use for the input's content.
     * @param type The data type of the resulting input (by default string).
     * @return The input.
     */
    public static AnyContent createInputItem(String name, String value, ValueEmbeddingEnumeration embeddingType, String type) {
        return createInputItem(name, value, embeddingType, type, null);
    }

    /**
     * Create an input item of for the provided information.
     *
     * @param name The input's name.
     * @param value The input's value.
     * @param embeddingType The embedding method to use for the input's content.
     * @param type The data type of the resulting input (by default string).
     * @param encoding The encoding to use (in case of character streams).
     * @return The input.
     */
    public static AnyContent createInputItem(String name, String value, ValueEmbeddingEnumeration embeddingType, String type, String encoding) {
        AnyContent input = new AnyContent();
        input.setName(name);
        input.setValue(value);
        input.setEmbeddingMethod(embeddingType);
        input.setType(type);
        input.setEncoding(encoding);
        return input;
    }

    /**
     * Check that the requested locale is supported and if not return a default locale to use.
     *
     * @param requestedLocale The requested locale.
     * @param domainConfig The domain configuration.
     * @return The locale to use.
     */
    public static Locale getSupportedLocale(Locale requestedLocale, DomainConfig domainConfig) {
        if (requestedLocale == null || requestedLocale.getLanguage() == null) {
            return domainConfig.getDefaultLocale();
        } else {
            if (domainConfig.getAvailableLocales().contains(requestedLocale)) {
                return requestedLocale;
            } else {
                return domainConfig.getDefaultLocale();
            }
        }
    }

    /**
     * Read a TAR instance from the provided TAR XML file.
     *
     * @param tarXml The XML file to read.
     * @return The TAR instance.
     */
    public static TAR toTAR(File tarXml) {
        try (FileInputStream inputStream = new FileInputStream(tarXml)) {
            return toTAR(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to deserialize TAR report from file", e);
        }
    }

    /**
     * Read a TAR instance from the provided TAR XML input stream.
     *
     * @param tarXml The XML stream to read.
     * @return The TAR instance.
     */
    public static TAR toTAR(InputStream tarXml) {
        try {
            Unmarshaller unmarshaller = tdlJaxbContext.createUnmarshaller();
            JAXBElement<TAR> tar = unmarshaller.unmarshal(new StreamSource(tarXml), TAR.class);
            return tar.getValue();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize TAR report from stream", e);
        }
    }

    /**
     * Convert the provided detailed TAR report to an aggregate report.
     *
     * @param detailedTAR The detailed report to consider.
     * @param helper The localisation helper to lookup translations.
     * @return The aggregated report.
     */
    public static TAR toAggregatedTAR(TAR detailedTAR, LocalisationHelper helper) {
        var aggregatedTAR = new TAR();
        if (detailedTAR.getCounters() != null) {
            aggregatedTAR.setCounters(new ValidationCounters());
            aggregatedTAR.getCounters().setNrOfAssertions(BigInteger.valueOf(detailedTAR.getCounters().getNrOfAssertions().longValue()));
            aggregatedTAR.getCounters().setNrOfErrors(BigInteger.valueOf(detailedTAR.getCounters().getNrOfErrors().longValue()));
            aggregatedTAR.getCounters().setNrOfWarnings(BigInteger.valueOf(detailedTAR.getCounters().getNrOfWarnings().longValue()));
        }
        aggregatedTAR.setDate(detailedTAR.getDate());
        aggregatedTAR.setName(detailedTAR.getName());
        aggregatedTAR.setResult(detailedTAR.getResult());
        if (detailedTAR.getContext() != null) {
            aggregatedTAR.setContext(new AnyContent());
        }
        if (detailedTAR.getReports() != null) {
            aggregatedTAR.setReports(new TestAssertionGroupReportsType());
            var aggregatedReportItems = new AggregateReportItems(new ObjectFactory(), helper);
            for (var item: detailedTAR.getReports().getInfoOrWarningOrError()) {
                aggregatedReportItems.updateForReportItem(item);
            }
            aggregatedTAR.getReports().getInfoOrWarningOrError().addAll(aggregatedReportItems.getReportItems());
        }
        return aggregatedTAR;
    }

    /**
     * Check to see if the provided aggregated and detailed reports have differences (i.e. whether there is any aggregation).
     *
     * @param detailedTAR The detailed report.
     * @param aggregateTAR The aggregated report.
     * @return The check result.
     */
    public static boolean aggregateDiffers(TAR detailedTAR, TAR aggregateTAR) {
        return detailedTAR != null && aggregateTAR != null &&
                detailedTAR.getReports() != null && aggregateTAR.getReports() != null &&
                detailedTAR.getReports().getInfoOrWarningOrError().size() != aggregateTAR.getReports().getInfoOrWarningOrError().size();
    }

    /**
     * If needed limit the number of findings in the report.
     *
     * @param report The report.
     * @param domainConfig The domain configuration.
     * @param <R> The domain config type.
     */
    public static <R extends DomainConfig> void limitReportItemsIfNeeded(TAR report, R domainConfig) {
        // Apply detailed report limit for report items (if needed).
        if (report.getReports() != null && report.getReports().getInfoOrWarningOrError().size() > domainConfig.getMaximumReportsForXmlOutput()) {
            report.getReports().getInfoOrWarningOrError().subList(domainConfig.getMaximumReportsForXmlOutput().intValue(), report.getReports().getInfoOrWarningOrError().size()).clear();
        }
    }

}
