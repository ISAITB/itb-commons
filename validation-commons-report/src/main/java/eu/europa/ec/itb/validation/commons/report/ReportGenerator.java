package eu.europa.ec.itb.validation.commons.report;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.*;
import eu.europa.ec.itb.validation.commons.report.dto.ContextItem;
import eu.europa.ec.itb.validation.commons.report.dto.Report;
import eu.europa.ec.itb.validation.commons.report.dto.ReportItem;
import net.sf.jasperreports.engine.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class used to prepare and create PDF reports.
 */
public class ReportGenerator {

    private final static Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);
    private final JAXBContext jaxbContext;

    /**
     * Constructor.
     */
    public ReportGenerator() {
        try {
            jaxbContext = JAXBContext.newInstance(
                    TAR.class,
                    TestCaseReportType.class,
                    TestStepStatus.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Use the provided report file stream to generate the report's output.
     *
     * @param reportStream The stream for the report template to use.
     * @param parameters The parameters to use for the report population.
     * @param outputStream The stream on which to write the generated report.
     * @throws JRException If the PDF file failed generation.
     */
    private void writeReport(InputStream reportStream, Map<String, Object> parameters, OutputStream outputStream) throws JRException {
        JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, new JREmptyDataSource());
        try {
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    LOG.warn("Error while closing report stream", e);
                }
            }
        }
    }

    /**
     * Create a report based on a provided classpath location for the report template to use.
     *
     * @param reportPath The classpath of the report template to use.
     * @param parameters The parameters for the report's population.
     * @param outputStream The stream on which to write the report.
     * @throws JRException If the PDF file failed generation.
     */
    private void writeClasspathReport(String reportPath, Map<String, Object> parameters, OutputStream outputStream) throws JRException {
        writeReport(Thread.currentThread().getContextClassLoader().getResourceAsStream(reportPath), parameters, outputStream);
    }

    /**
     * Create a TAR PDF report. This also adds the context information to the report.
     *
     * @param inputStream The stream for the TAR XML report to use as input.
     * @param title The title to use in the PDF document.
     * @param outputStream The stream on which to write the generated report.
     */
    public void writeTARReport(InputStream inputStream, String title, OutputStream outputStream) {
        writeTARReport(inputStream, title, outputStream, true);
    }

    /**
     * Create a TAR PDF report. This also adds the context information to the report.
     *
     * @param reportType The TAR report to use as input.
     * @param title The title to use in the PDF document.
     * @param outputStream The stream on which to write the generated report.
     */
    public void writeTARReport(TAR reportType, String title, OutputStream outputStream) {
        writeTARReport(reportType, title, outputStream, true);
    }

    /**
     * Create a TAR PDF report.
     *
     * @param reportType The TAR report to use as input.
     * @param title The title to use in the PDF document.
     * @param outputStream The stream on which to write the generated report.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     */
    public void writeTARReport(TAR reportType, String title, OutputStream outputStream, boolean addContext) {
        writeTestStepReport(reportType, title, outputStream, addContext);
    }

    /**
     * Create a TAR PDF report.
     *
     * @param inputStream The stream to read the TAR XML report as input.
     * @param title The title to use in the PDF document.
     * @param outputStream The stream on which to write the generated report.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     */
    public void writeTARReport(InputStream inputStream, String title, OutputStream outputStream, boolean addContext) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TAR> tar = unmarshaller.unmarshal(new StreamSource(inputStream), TAR.class);
            writeTARReport(tar.getValue(), title, outputStream, addContext);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Write the report of an abstract report.
     *
     * @param reportType The input for the specific report type.
     * @param title The title to use in the PDF document.
     * @param outputStream The stream on which to write the generated report.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     */
    private void writeTestStepReport(TestStepReportType reportType, String title, OutputStream outputStream, boolean addContext) {
        try {
            Report report = fromTestStepReportType(reportType, title, addContext);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("title", report.getTitle());
            parameters.put("reportDate", report.getReportDate());
            parameters.put("reportResult", report.getReportResult());
            parameters.put("errorCount", report.getErrorCount());
            parameters.put("warningCount", report.getWarningCount());
            parameters.put("messageCount", report.getMessageCount());
            if (report.getReportItems() != null && !report.getReportItems().isEmpty()) {
                parameters.put("reportItems", report.getReportItems());
            }
            if (report.getContextItems() != null && !report.getContextItems().isEmpty()) {
                parameters.put("contextItems", report.getContextItems());
            }
            writeClasspathReport("reports/TAR.jasper", parameters, outputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Add a context item to the list of DTOs to process.
     *
     * @param context The TAR report's context map.
     * @param items The collected DTOs.
     * @param keyPath The path to the context item to lookup.
     */
    private void addContextItem(AnyContent context, List<ContextItem> items, String keyPath) {
        if ("map".equals(context.getType()) || "list".equals(context.getType())) {
            if (context.getItem() != null) {
                if (!"".equals(keyPath)) {
                    keyPath += ".";
                }
                for (AnyContent internalContext: context.getItem()) {
                    addContextItem(internalContext, items, keyPath + context.getName());
                }
            }
        } else {
            if (context.getValue() != null) {
                if (!"".equals(keyPath)) {
                    keyPath += ".";
                }
                ContextItem item = new ContextItem();
                item.setKey(keyPath+context.getName());
                if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
                    item.setValue(context.getValue());
                } else if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.URI) {
                    item.setValue("[URI: "+context.getValue()+"]");
                } else if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64) {
                    item.setValue("[BASE64 content]");
                }
                items.add(item);
            }
        }
    }

    /**
     * Create a report DTO from the provided report information.
     *
     * @param reportType The report's information.
     * @param title The title to use in the PDF document.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     * @param <T> The specific report class.
     * @return The report DTO.
     */
    private <T extends TestStepReportType> Report fromTestStepReportType(T reportType, String title, boolean addContext) {
        Report report = new Report();
        if (reportType.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'Z'");
            sdf.setTimeZone(TimeZone.getDefault());
            report.setReportDate(sdf.format(reportType.getDate().toGregorianCalendar().getTime()));
        }
        report.setReportResult(reportType.getResult().value());
        report.setTitle(Objects.requireNonNullElse(title, "Report"));
        if (reportType instanceof TAR) {
            TAR tarReport = (TAR)reportType;
            if (addContext) {
                for (AnyContent context : tarReport.getContext().getItem()) {
                    addContextItem(context, report.getContextItems(), "");
                }
            }
            if (tarReport.getReports() != null && tarReport.getReports().getInfoOrWarningOrError() != null) {
                int errors = 0;
                int warnings = 0;
                int messages = 0;
                for (JAXBElement<TestAssertionReportType> element : tarReport.getReports().getInfoOrWarningOrError()) {
                    if (element.getValue() instanceof BAR) {
                        BAR tarItem = (BAR) element.getValue();
                        ReportItem reportItem = new ReportItem();
                        reportItem.setLevel(element.getName().getLocalPart());
                        if ("error".equalsIgnoreCase(reportItem.getLevel())) {
                            errors += 1;
                        } else if ("warning".equalsIgnoreCase(reportItem.getLevel())) {
                            warnings += 1;
                        } else {
                            messages += 1;
                        }
                        reportItem.setDescription(StringUtils.defaultIfBlank(tarItem.getDescription(), "-"));
                        reportItem.setTest(StringUtils.trimToNull(tarItem.getTest()));
                        reportItem.setLocation(StringUtils.trimToNull(tarItem.getLocation()));
                        report.getReportItems().add(reportItem);
                    }
                }
                report.setErrorCount(String.valueOf(errors));
                report.setWarningCount(String.valueOf(warnings));
                report.setMessageCount(String.valueOf(messages));
            }
        }
        if (report.getReportItems().isEmpty()) {
            report.setReportItems(null);
        }
        if (report.getContextItems().isEmpty()) {
            report.setContextItems(null);
        }
        return report;
    }

}
