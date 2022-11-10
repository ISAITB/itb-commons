package eu.europa.ec.itb.validation.commons.war.webhook;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;

import eu.europa.ec.itb.validation.commons.web.WebServiceContextProvider;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;

/** 
 * Class with statistics reporting functionalities common to all the validators.
 * This class is expected to be subclassed in each validator by the Singleton class triggering the statistics reporting.
*/
public abstract class StatisticReporting {

    private static final Logger logger = LoggerFactory.getLogger(StatisticReporting.class);
    private static final ThreadLocal<Map<String, String>> adviceContext = new ThreadLocal<>();

    private DatabaseReader reader; 

	@Autowired
	protected ApplicationConfig config;

	@Autowired
	private WebHook webHook;

    /**
     * If enabled to do so, initialise the IP to country database reader. If any issue occurs while doing so
     * the country extraction will be disabled.
     */
    @PostConstruct
    public void init(){
        if(config.getWebhook().isStatisticsEnableCountryDetection()){
            String geoipDbFilePath = config.getWebhook().getStatisticsCountryDetectionDbFile();
            try {
                File geoipDbFile = new File(geoipDbFilePath);
                this.reader = new DatabaseReader.Builder(geoipDbFile).build();
            } catch(Exception ex) {
                logger.warn(String.format("Error accessing Geolocation database file at: %s", geoipDbFilePath), ex);
                // deactivate the country resolution if the database cannot be read
                this.config.getWebhook().setStatisticsEnableCountryDetection(Boolean.FALSE);
            }
        }
    }

    /**
     * Method that extracts the IP address from the HTTP request.
     *
     * @param request The HTTP request.
     * @return The IP address
     */
    public String extractIpAddress(HttpServletRequest request){
        String proxyIpHeader = config.getWebhook().getIpHeader();
        String ip = request.getHeader(proxyIpHeader);
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Method that obtains the country ISO.
     *
     * @param ip The IP address.
     * @return The ISO country code.
     */
    public String getCountryISO(String ip){
        String countryISO = "";
        try {
            InetAddress address = InetAddress.getByName(ip);
            CountryResponse response = this.reader.country(address);
            countryISO = response.getCountry().getIsoCode();
        } catch (AddressNotFoundException ex){
            logger.warn(String.format("Unable to resolve country for ip: %s", ip));
        } catch (Exception ex){
            logger.warn(String.format("Error when resolving country for ip: %s", ip), ex);
        }
        return countryISO;
    }

    /**
     * Method that sends the usage data using the webhook.
     *
     * @param validatorId The identifier of the validator.
     * @param domain The domain name.
     * @param api The API used.
     * @param validationType The validation type.
     * @param result The validation result.
     * @param ip The IP address.
     */
    public void sendUsageData(String validatorId, String domain, String api, String validationType, UsageData.Result result, String ip){
        String countryISO = null;
        if (config.getWebhook().isStatisticsEnableCountryDetection() && ip != null) {
            countryISO = getCountryISO(ip);
        }
        webHook.sendUsageData(new UsageData(validatorId, domain, api, validationType, result, countryISO));
    }

    /**
	 * Method that extracts the HttpServletRequest from the REST and WEB requests. This is used as support to AOP advice
     * methods.
     *
     * @param joinPoint The JoinPoint to process.
     * @return The HTTP request.
	 */
	protected HttpServletRequest getHttpRequest(JoinPoint joinPoint){
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		Class[] paramTypes = signature.getParameterTypes();
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] == HttpServletRequest.class) {
				Object[] args = joinPoint.getArgs();
				return (HttpServletRequest)args[i];
			}
		}
		logger.warn("Unexpected execution point reached: HttpServletRequest object not found among API arguments.");
		return null;
	}

    /**
     * Common advice processing for all web UIs.
     *
     * @param joinPoint The relevant join point.
     * @param api The specific API type.
     */
    protected void handleUploadContext(JoinPoint joinPoint, String api) {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put(StatisticReportingConstants.PARAM_API, api);
        if (config.getWebhook().isStatisticsEnableCountryDetection()) {
            HttpServletRequest request = getHttpRequest(joinPoint);
            if (request != null) {
                String ip = extractIpAddress(request);
                contextParams.put(StatisticReportingConstants.PARAM_IP, ip);
            }
        }
        adviceContext.set(contextParams);
    }

    /**
     * Common advice processing for all SOAP API calls.
     *
     * @param joinPoint The original call's information.
     */
    protected void handleSoapCallContext(JoinPoint joinPoint) {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put(StatisticReportingConstants.PARAM_API, StatisticReportingConstants.SOAP_API);
        if (config.getWebhook().isStatisticsEnableCountryDetection() && joinPoint.getTarget() instanceof WebServiceContextProvider) {
            var validationService = (WebServiceContextProvider)joinPoint.getTarget();
            HttpServletRequest request = (HttpServletRequest)validationService.getWebServiceContext().getMessageContext()
                    .get(MessageContext.SERVLET_REQUEST);
            String ip = extractIpAddress(request);
            contextParams.put(StatisticReportingConstants.PARAM_IP, ip);
        }
        adviceContext.set(contextParams);
    }

    /**
     * Common advice processing for all REST API calls.
     *
     * @param joinPoint The original call's information.
     */
    protected void handleRestCallContext(JoinPoint joinPoint) {
        String ip;
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put(StatisticReportingConstants.PARAM_API, StatisticReportingConstants.REST_API);
        if (config.getWebhook().isStatisticsEnableCountryDetection()) {
            HttpServletRequest request = getHttpRequest(joinPoint);
            if (request != null) {
                ip = extractIpAddress(request);
                contextParams.put(StatisticReportingConstants.PARAM_IP, ip);
            }
        }
        adviceContext.set(contextParams);
    }

    /**
     * Common advice processing for all validations via email.
     *
     * @param joinPoint The original call's information.
     */
    protected void handleEmailContext(JoinPoint joinPoint) {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put(StatisticReportingConstants.PARAM_API, StatisticReportingConstants.EMAIL_API);
        adviceContext.set(contextParams);
    }

    /**
     * Get the map with context information to use for the statistics call.
     *
     * @return The map.
     */
    protected Map<String, String> getAdviceContext() {
        var contextMap = adviceContext.get();
        adviceContext.remove();
        return contextMap;
    }

}
