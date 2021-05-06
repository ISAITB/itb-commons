package eu.europa.ec.itb.commons.war.webhook;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonAppend;

/**
 * @version 1.0.0-SNAPSHOT
 * 
 *          Class that holds the usage data attributes.
 */
@JsonAppend(attrs = {
	@JsonAppend.Attr(value = "secret")
})
public class UsageData {

	private String validator;
	private String domain;
	private String api;
	private String validationType;
	private Result result;
	@JsonFormat(pattern = "yyyy-MM-dd'T'hh:mm:ss")
	private Date validationTime;

	public UsageData(String validator, String domain, String api, String validationType, Result result) {
		this.validator = validator;
		this.domain = domain;
		this.api = api;
		this.validationType = validationType;
		this.result = result;
		this.validationTime = new Date();
	}

	public String getValidator() {
		return this.validator;
	}
	
	public String getDomain() {
		return this.domain;
	}

	public String getApi() {
		return this.api;
	}

	public String getValidationType() {
		return this.validationType;
	}

	public Result getResult() {
		return this.result;
	}

	public Date getValidationTime() {
		return this.validationTime;
	}


	/**
	 * ENUM to control the values fo the validation results.
	 */
	public enum Result {

		SUCCESS("success"), WARNING("warning"), FAILURE("failure");

		private String name;

		Result(String name) {
			this.name = name;
		}

		public String toString() {
			return this.name;
		}
	}

}
