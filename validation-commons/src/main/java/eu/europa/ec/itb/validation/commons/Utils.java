package eu.europa.ec.itb.validation.commons;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;
import com.gitb.vs.ValidateRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

/**
 * Class holding utility methods for common operations.
 */
public class Utils {

    /**
     * A key used to record line numbers in parsed XML content.
     */
    public static String LINE_NUMBER_KEY_NAME = "lineNumber";

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
     * Serialise the provided node to a byte array.
     *
     * @param content The content.
     * @return the node's bytes.
     */
    public static byte[] serialize(Node content) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.transform(new DOMSource(content), new StreamResult(baos));
        } catch (TransformerException te) {
            throw new IllegalStateException(te);
        }
        return baos.toByteArray();
    }

    /**
     * Create an empty XML DOM document.
     *
     * @return The document.
     */
    public static Document emptyDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
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
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<>();
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
            public void characters(final char ch[], final int start, final int length) throws SAXException {
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
        if (validateRequest != null) {
            if (validateRequest.getInput() != null) {
                inputs.addAll(getInputFor(validateRequest.getInput(), name));
            }
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
                    if (report.getResult() != null && report.getResult() != TestResultType.UNDEFINED) {
                        if ((mergedReport.getResult() == TestResultType.UNDEFINED) ||
                                (mergedReport.getResult() == TestResultType.SUCCESS && report.getResult() != TestResultType.SUCCESS) ||
                                (mergedReport.getResult() == TestResultType.WARNING && report.getResult() == TestResultType.FAILURE)) {
                            mergedReport.setResult(report.getResult());
                        }
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

}
