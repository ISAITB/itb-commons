/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.validation.commons.report;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.tr.TestStepReportType;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.impl.FSDefaultCacheStore;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.util.XRLog;
import eu.europa.ec.itb.validation.commons.ReportProperties;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.report.dto.ContextItem;
import eu.europa.ec.itb.validation.commons.report.dto.Report;
import eu.europa.ec.itb.validation.commons.report.dto.ReportItem;
import eu.europa.ec.itb.validation.commons.report.dto.ReportLabels;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Class used to prepare and create PDF reports.
 */
public class ReportGenerator {

    static {
        // Use SLF4J logging.
        XRLog.setLoggerImpl(new Slf4jLogger());
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);
    private final Map<String, TemplateMethodModelEx> extensionFunctions;
    private final FSCacheEx<String, FSCacheValue> fontCache = new FSDefaultCacheStore();
    private Template reportTemplate;

    /**
     * Constructor.
     */
    public ReportGenerator() {
        extensionFunctions = Map.of(
            "escape", arguments -> {
                if (arguments != null && !arguments.isEmpty()) {
                    var text = arguments.get(0);
                    return StringEscapeUtils.escapeHtml4(String.valueOf(text));
                }
                return null;
            }
        );
        // Preload the report template to use.
        getTemplate();
    }

    /**
     * Create a TAR PDF report. This also adds the context information to the report.
     *
     * @param inputStream The stream for the TAR XML report to use as input.
     * @param outputStream The stream on which to write the generated report.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeTARReport(InputStream inputStream, OutputStream outputStream, Function<TAR, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        writeTARReport(inputStream, outputStream, true, labelProvider, properties, domainConfig);
    }

    /**
     * Create a TAR PDF report. This also adds the context information to the report.
     *
     * @param reportType The TAR report to use as input.
     * @param outputStream The stream on which to write the generated report.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeTARReport(TAR reportType, OutputStream outputStream, Function<TAR, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        writeTARReport(reportType, outputStream, true, labelProvider, properties, domainConfig);
    }

    /**
     * Create a TAR PDF report.
     *
     * @param reportType The TAR report to use as input.
     * @param outputStream The stream on which to write the generated report.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeTARReport(TAR reportType, OutputStream outputStream, boolean addContext, Function<TAR, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        writeTestStepReport(reportType, outputStream, addContext, labelProvider, properties, domainConfig);
    }

    /**
     * Create a TAR PDF report.
     *
     * @param inputStream The stream to read the TAR XML report as input.
     * @param outputStream The stream on which to write the generated report.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    public <R extends DomainConfig> void writeTARReport(InputStream inputStream, OutputStream outputStream, boolean addContext, Function<TAR, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        writeTARReport(Utils.toTAR(inputStream), outputStream, addContext, labelProvider, properties, domainConfig);
    }

    /**
     * Load the report template to use.
     *
     * @return The template.
     */
    private Template getTemplate() {
        if (reportTemplate == null) {
            var configuration = new Configuration(Configuration.VERSION_2_3_32);
            configuration.setTemplateLoader(new ClassTemplateLoader(ReportGenerator.class, "/"));
            try {
                reportTemplate = configuration.getTemplate("reports/TAR.ftl");
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load report template", e);
            }
        }
        return reportTemplate;
    }

    private void loadFonts(PdfRendererBuilder builder) {
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSans.ttf"), "FreeSans", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansBold.ttf"), "FreeSans", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansOblique.ttf"), "FreeSans", 400, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansOblique.ttf"), "FreeSans", 400, BaseRendererBuilder.FontStyle.OBLIQUE, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansBoldOblique.ttf"), "FreeSans", 700, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansBoldOblique.ttf"), "FreeSans", 700, BaseRendererBuilder.FontStyle.OBLIQUE, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMono.ttf"), "FreeMono", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoBold.ttf"), "FreeMono", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoOblique.ttf"), "FreeMono", 400, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoOblique.ttf"), "FreeMono", 400, BaseRendererBuilder.FontStyle.OBLIQUE, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoBoldOblique.ttf"), "FreeMono", 700, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoBoldOblique.ttf"), "FreeMono", 700, BaseRendererBuilder.FontStyle.OBLIQUE, true);
    }

    /**
     * Create a report based on a provided classpath location for the report template to use.
     *
     * @param parameters The parameters for the report's population.
     * @param outputStream The stream on which to write the report.
     */
    private void writeClasspathReport(Map<String, Object> parameters, OutputStream outputStream) {
        // Add custom extension functions.
        parameters = Objects.requireNonNullElseGet(parameters, HashMap::new);
        parameters.putAll(extensionFunctions);
        // Generate HTML report.
        try {
            var writer = new StringWriter();
            getTemplate().process(parameters, writer);
            writer.flush();
            var tempHtmlString = writer.toString();
            if (LOG.isDebugEnabled()) {
                LOG.debug("### Report HTML - START ###\n\n{}\n\n### Report HTML - END ###", tempHtmlString);
            }
            // Convert to PDF.
            var builder = new PdfRendererBuilder();
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.useCacheStore(PdfRendererBuilder.CacheStore.PDF_FONT_METRICS, fontCache);
            loadFonts(builder);
            builder.useUriResolver(new NaiveUserAgent.DefaultUriResolver() {
                @Override
                public String resolveURI(String baseUri, String uri) {
                    if (uri.startsWith("classpath:")) {
                        // A predefined image.
                        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(Strings.CS.removeStart(uri, "classpath:"))).toString();
                    }
                    return super.resolveURI(baseUri, uri);
                }
            });
            builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(tempHtmlString)), "reports");
            builder.toStream(outputStream);
            builder.run();
        } catch (Exception e) {
            throw new IllegalStateException("Error while generating report", e);
        }
    }

    /**
     * Write the report of an abstract report.
     *
     * @param reportType The input for the specific report type.
     * @param outputStream The stream on which to write the generated report.
     * @param addContext True if the context information from the TAR object should also be added to the PDF output.
     * @param labelProvider A function to provide the labels to use in the report.
     * @param properties The additional properties to consider when generating the report.
     * @param domainConfig The domain configuration.
     */
    private <T extends TestStepReportType, R extends DomainConfig> void writeTestStepReport(T reportType, OutputStream outputStream, boolean addContext, Function<T, ReportLabels> labelProvider, ReportProperties properties, R domainConfig) {
        try {
            var labels = labelProvider.apply(reportType);
            Report report = fromTestStepReportType(reportType, labels.getTitle(), addContext);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("title", report.getTitle());
            parameters.put("reportDate", report.getReportDate());
            parameters.put("reportResult", report.getReportResult());
            parameters.put("overviewLabel", labels.getOverview());
            parameters.put("detailsLabel", labels.getDetails());
            parameters.put("resultLabel", labels.getResult());
            parameters.put("resultTypeLabel", labels.getResultType());
            parameters.put("dateLabel", labels.getDate());
            parameters.put("testLabel", labels.getTest());
            parameters.put("locationLabel", labels.getLocation());
            parameters.put("pageLabel", labels.getPage());
            parameters.put("ofLabel", labels.getOf());
            parameters.put("assertionIdLabel", labels.getAssertionId());
            parameters.put("resultFindingsLabel", labels.getFindings());
            parameters.put("resultFindingsDetailsLabel", labels.getFindingsDetails());
            parameters.put("richTextReportItems", domainConfig.isRichTextReports());
            parameters.put("errorsLabel", labels.getErrors());
            parameters.put("warningsLabel", labels.getWarnings());
            parameters.put("messagesLabel", labels.getMessages());
            parameters.put("errorSectionTitle", labels.getErrorSectionTitle());
            parameters.put("warningSectionTitle", labels.getWarningSectionTitle());
            parameters.put("messageSectionTitle", labels.getMessageSectionTitle());
            if (properties != null) {
                // File name.
                parameters.put("fileNameLabel", labels.getFileName());
                parameters.put("reportFileName", properties.inputFileName());
                // Validation type.
                parameters.put("validationTypeLabel", labels.getValidationType());
                parameters.put("validationTypeName", labels.getValidationTypeName());
            }
            // Custom message.
            if (labels.getCustomMessageOverview() != null) parameters.put("customMessageOverview", labels.getCustomMessageOverview());
            if (labels.getCustomMessageErrors() != null) parameters.put("customMessageErrors", labels.getCustomMessageErrors());
            if (labels.getCustomMessageWarnings() != null) parameters.put("customMessageWarnings", labels.getCustomMessageWarnings());
            if (labels.getCustomMessageMessages() != null) parameters.put("customMessageMessages", labels.getCustomMessageMessages());
            int errorCountUnique = 0;
            int warningCountUnique = 0;
            int messageCountUnique = 0;
            if (report.getReportItems() != null && !report.getReportItems().isEmpty()) {
                parameters.put("reportItems", report.getReportItems());
                var splitReport = report.splitPerSeverity();
                errorCountUnique = splitReport.errors().size();
                warningCountUnique = splitReport.warnings().size();
                messageCountUnique = splitReport.messages().size();
                if (!splitReport.errors().isEmpty()) parameters.put("errorItems", splitReport.errors());
                if (!splitReport.warnings().isEmpty()) parameters.put("warningItems", splitReport.warnings());
                if (!splitReport.messages().isEmpty()) parameters.put("messageItems", splitReport.messages());
            }
            parameters.put("errorCount", getFindingCountMessage(report.getErrorCount(), errorCountUnique, labels));
            parameters.put("warningCount", getFindingCountMessage(report.getWarningCount(), warningCountUnique, labels));
            parameters.put("messageCount", getFindingCountMessage(report.getMessageCount(), messageCountUnique, labels));
            parameters.put("totalCount", getFindingCountMessage(report.getErrorCount()+report.getWarningCount()+report.getMessageCount(), errorCountUnique+warningCountUnique+messageCountUnique, labels));
            if (report.getContextItems() != null && !report.getContextItems().isEmpty()) {
                parameters.put("contextItems", report.getContextItems());
            }
            writeClasspathReport(parameters, outputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create the count message for the provided findings totals.
     *
     * @param totalCount The totals.
     * @param uniqueCount The unique rules.
     * @param labels The labels to use.
     * @return the message.
     */
    private String getFindingCountMessage(int totalCount, int uniqueCount, ReportLabels labels) {
        if (totalCount == uniqueCount) {
            return String.valueOf(totalCount);
        } else {
            if (uniqueCount == 1) {
                return "%s (%s)".formatted(totalCount, labels.getUniqueRule().replace("{0}", String.valueOf(uniqueCount)));
            } else {
                return "%s (%s)".formatted(totalCount, labels.getUniqueRules().replace("{0}", String.valueOf(uniqueCount)));
            }
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
    <T extends TestStepReportType> Report fromTestStepReportType(T reportType, String title, boolean addContext) {
        Report report = new Report();
        if (reportType.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getDefault());
            report.setReportDate(sdf.format(reportType.getDate().toGregorianCalendar().getTime()));
        }
        report.setReportResult(reportType.getResult().value());
        report.setTitle(Objects.requireNonNullElse(title, "Report"));
        if (reportType instanceof TAR tarReport) {
            if (addContext && tarReport.getContext() != null) {
                for (AnyContent context : tarReport.getContext().getItem()) {
                    addContextItem(context, report.getContextItems(), "");
                }
            }
            int errors = 0;
            int warnings = 0;
            int messages = 0;
            if (tarReport.getReports() != null && tarReport.getReports().getInfoOrWarningOrError() != null) {
                for (JAXBElement<TestAssertionReportType> element : tarReport.getReports().getInfoOrWarningOrError()) {
                    if (element.getValue() instanceof BAR tarItem) {
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
                        reportItem.setAssertionId(StringUtils.trimToNull(tarItem.getAssertionID()));
                        report.getReportItems().add(reportItem);
                    }
                }
            }
            // If an explicit counters object is defined override the calculated ones.
            if (tarReport.getCounters() != null
                    && tarReport.getCounters().getNrOfErrors() != null
                    && tarReport.getCounters().getNrOfWarnings() != null
                    && tarReport.getCounters().getNrOfAssertions() != null) {
                errors = tarReport.getCounters().getNrOfErrors().intValue();
                warnings = tarReport.getCounters().getNrOfWarnings().intValue();
                messages = tarReport.getCounters().getNrOfAssertions().intValue();
            }
            report.setErrorCount(errors);
            report.setWarningCount(warnings);
            report.setMessageCount(messages);
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
