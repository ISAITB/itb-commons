/*
 * Copyright (C) 2025 European Union
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
    private String test;
    private String location;
    private String page;
    private String of;
    private String assertionId;
    private String findings;
    private String findingsDetails;

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

    /**
     * @return The findings label.
     */
    public String getFindings() {
        return findings;
    }

    /**
     * @param findings The findings label.
     */
    public void setFindings(String findings) {
        this.findings = findings;
    }

    /**
     * @return The findings details label.
     */
    public String getFindingsDetails() {
        return findingsDetails;
    }

    /**
     * @param findingsDetails The findings details label.
     */
    public void setFindingsDetails(String findingsDetails) {
        this.findingsDetails = findingsDetails;
    }
}
