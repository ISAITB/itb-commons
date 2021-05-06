package eu.europa.ec.itb.commons.war.webhook;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;

/**
 * @version 1.0.0-SNAPSHOT
 * 
 *          Singleton class that sends usage data for statistical purposes
 *          during every validation. It will only be instantiated if the URL
 *          property is declared.
 */
@Component
@EnableAsync
@ConditionalOnProperty(name = "validator.webhook.statistics")
public class WebHook {

	private static final Logger logger = LoggerFactory.getLogger(WebHook.class);

	@Autowired
	private ApplicationConfig config;

	@Autowired
	private ObjectMapper objectMapper;

	private ObjectWriter usageDataWriter;
	
	private RestTemplate restTemplate;

	private String url;

	private String secret;

	/**
	 * Method called to initialize the webhook attributes
	 */
	@PostConstruct
	public void initializeAttributes() throws MalformedURLException {
		// Instantiation of the rest template object to perform the calls
		this.restTemplate = new RestTemplate();
		// Gathering the properties to report usage
		ApplicationConfig.Webhook webhookConfig = config.getWebhook();
		this.url = webhookConfig.getStatistics();
		try {
			new URL(this.url);
		}catch(Exception ex) {
			throw new MalformedURLException(
					"The following URL for the usage statistics service is not valid: " + this.url);
		}
		this.secret = webhookConfig.getStatisticsSecret();
		if(this.secret == null){
			this.usageDataWriter = objectMapper.writerFor(UsageData.class);
		}else{
			this.usageDataWriter = objectMapper.writerFor(UsageData.class).withAttribute("secret", this.secret);
		}
		logger.info(String.format("Statistics reporting is active and set to post data to [%s]", this.url));
	}
	

	/**
	 * Method that posts the current validation details to the usage statistics
	 * service
	 * 
	 * @param data
	 */
	@Async
	public void sendUsageData(UsageData data) {
		Map<String, String> urlParams = new HashMap<String, String>();
		if (this.secret != null) {
			urlParams.put("secret", this.secret);
		}
		String jsonData;
		try {
			jsonData = usageDataWriter.writeValueAsString(data);
			logger.debug("Sending usage data:\n {}", jsonData);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);
			try{
				ResponseEntity<String> response = restTemplate.postForEntity(this.url, entity, String.class, urlParams);
				if(response.getStatusCodeValue() != 200) {
					logger.warn("Statistics reporting received response " + response.getStatusCodeValue() + "and message " + response.getBody());
				}
			}catch(Exception ex){
				logger.warn("Error during statistics reporting", ex);
			}
		} catch (JsonProcessingException ex) {
			logger.warn("Error serializing UsageData object to JSON format.", ex);
		}
	}

}
