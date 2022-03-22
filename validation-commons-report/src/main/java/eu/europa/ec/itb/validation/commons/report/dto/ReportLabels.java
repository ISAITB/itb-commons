package eu.europa.ec.itb.validation.commons.report.dto;

/**
 * Class that holds the labels to be used when generating PDF reports.
 */
public class ReportLabels {

    private String title;
    private String overview;
    private String details;
    private String date;
    private String result;
    private String resultType;
    private String fileName;
    private String errors;
    private String warnings;
    private String messages;
    private String test;
    private String location;
    private String page;
    private String of;
    private String assertionId;

    /**
     * @return The title of the report.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title of the report.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The overview label.
     */
    public String getOverview() {
        return overview;
    }

    /**
     * @param overview The overview label.
     */
    public void setOverview(String overview) {
        this.overview = overview;
    }

    /**
     * @return The details label.
     */
    public String getDetails() {
        return details;
    }

    /**
     * @param details The details label.
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * @return The date label.
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date The date label.
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * @return The result label.
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result The result label.
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return The file name label.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName The file name label.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return The errors label.
     */
    public String getErrors() {
        return errors;
    }

    /**
     * @param errors The errors label.
     */
    public void setErrors(String errors) {
        this.errors = errors;
    }

    /**
     * @return The warnings label.
     */
    public String getWarnings() {
        return warnings;
    }

    /**
     * @param warnings The warnings label.
     */
    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    /**
     * @return The messages label.
     */
    public String getMessages() {
        return messages;
    }

    /**
     * @param messages The messages label.
     */
    public void setMessages(String messages) {
        this.messages = messages;
    }

    /**
     * @return The test label.
     */
    public String getTest() {
        return test;
    }

    /**
     * @param test The test label.
     */
    public void setTest(String test) {
        this.test = test;
    }

    /**
     * @return The location label.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location The location label.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return The type of result.
     */
    public String getResultType() {
        return resultType;
    }

    /**
     * @param resultType The type of result.
     */
    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    /**
     * @return The page label.
     */
    public String getPage() {
        return page;
    }

    /**
     * @param page The page label.
     */
    public void setPage(String page) {
        this.page = page;
    }

    /**
     * @return The (page) of label.
     */
    public String getOf() {
        return of;
    }

    /**
     * @param of The (page) of label.
     */
    public void setOf(String of) {
        this.of = of;
    }

    /**
     * @return The assertion ID label.
     */
    public String getAssertionId() {
        return assertionId;
    }

    /**
     * @param assertionId The assertion ID label.
     */
    public void setAssertionId(String assertionId) {
        this.assertionId = assertionId;
    }
}
