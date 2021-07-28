package eu.europa.ec.itb.validation.commons.war.webhook;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonAppend;

/**
 * Class that holds the usage data attributes.
 */
@JsonAppend(attrs = {
        @JsonAppend.Attr(value = "secret")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageData {

    private final String validator;
    private final String domain;
    private final String api;
    private final String validationType;
    private final Result result;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final Date validationTime;
    @JsonInclude(Include.NON_EMPTY)
    private final String country;

    /**
     * Constructor.
     *
     * @param validator The validator identifier.
     * @param domain The domain.
     * @param api The API.
     * @param validationType The validation type.
     * @param result The validation result.
     */
    public UsageData(String validator, String domain, String api, String validationType, Result result) {
        this(validator, domain, api, validationType, result, null);
    }

    /**
     * Constructor.
     *
     * @param validator The validator identifier.
     * @param domain The domain.
     * @param api The API.
     * @param validationType The validation type.
     * @param result The validation result.
     * @param country The country code.
     */
    public UsageData(String validator, String domain, String api, String validationType, Result result, String country){
        this.validator = validator;
        this.domain = domain;
        this.api = api;
        this.validationType = validationType;
        this.result = result;
        this.validationTime = new Date();
        this.country = country;
    }

    /**
     * @return The validator identifier.
     */
    public String getValidator() {
        return this.validator;
    }

    /**
     * @return The domain.
     */
    public String getDomain() {
        return this.domain;
    }

    /**
     * @return The API.
     */
    public String getApi() {
        return this.api;
    }

    /**
     * @return The validation type.
     */
    public String getValidationType() {
        return this.validationType;
    }

    /**
     * @return The validation result.
     */
    public Result getResult() {
        return this.result;
    }

    /**
     * @return The validation time.
     */
    public Date getValidationTime() {
        return this.validationTime;
    }

    /**
     * @return The country code.
     */
    public String getCountry(){
        return this.country;
    }

    /**
     * ENUM to control the values fo the validation results.
     */
    public enum Result {

        /** Validation was successful. */
        SUCCESS("success"),
        /** Validation was successful but generated warnings. */
        WARNING("warning"),
        /** Validation failed. */
        FAILURE("failure");

        private final String name;

        /**
         * Constructor.
         *
         * @param name The result name.
         */
        Result(String name) {
            this.name = name;
        }

        /**
         * Get the result's string value.
         *
         * @return The enum's name.
         */
        public String toString() {
            return this.name;
        }
    }

}
