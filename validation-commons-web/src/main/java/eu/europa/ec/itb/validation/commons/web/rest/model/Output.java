package eu.europa.ec.itb.validation.commons.web.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The validator's output providing the produced validation report.
 */
@Schema(description = "The content and metadata linked to the validation report that corresponds to a provided RDF input.")
public class Output {

    @Schema(description = "The validation report, provided as a BASE64 encoded String.")
    private String report;

    /**
     * @return The validation report, provided as a BASE64 encoded String.
     */
    public String getReport() {
        return report;
    }

    /**
     * @param report The validation report, provided as a BASE64 encoded String.
     */
    public void setReport(String report) {
        this.report = report;
    }

}
