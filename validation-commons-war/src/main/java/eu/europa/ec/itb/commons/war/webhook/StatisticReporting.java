package eu.europa.ec.itb.commons.war.webhook;

import java.io.File;
import java.net.InetAddress;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;

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
    
    private DatabaseReader reader; 

	@Autowired
	protected ApplicationConfig config;

	@Autowired
	private WebHook webHook;

    @PostConstruct
    public void init(){
        if(config.getWebhook().isStatisticsEnableCountryDetection()){
            String geoipDbFilePath = config.getWebhook().getStatisticsCountryDetectionDbFile();
            try{
                File geoipDbFile = new File(geoipDbFilePath);
                this.reader = new DatabaseReader.Builder(geoipDbFile).build();
            }catch(Exception ex){
                logger.warn(String.format("Error accessing Geolocation database file at: %s", geoipDbFilePath), ex);
                // deactivate the country resolution if the database cannot be read
                this.config.getWebhook().setStatisticsEnableCountryDetection(Boolean.FALSE);
            }
        }
    }

    /**
     * Method that extracts the IP address from the HTTP request
     * @param ip
     * @return
     */
    public String extractIpAddress(HttpServletRequest request){
        String proxyIpHeader = config.getWebhook().getIpHeader();
        String ip = request.getHeader(proxyIpHeader);
        if(ip == null || ip.isEmpty()){
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Method that obtains the country ISO 
    */
    public String getCountryISO(String ip){
        String countryISO = "";
        try{
            InetAddress address = InetAddress.getByName(ip);
            CountryResponse response = this.reader.country(address);
            countryISO = response.getCountry().getIsoCode();    
        }catch(Exception ex){
            logger.warn(String.format("Error when resolving country for ip: %s", ip), ex);
        }
        return countryISO;
    }

    /**
     * Method that sends the usage data using the webhook
    */
    public void sendUsageData(String validatorId, String domain, String api, String validationType, UsageData.Result result, String ip){
        String countryISO = null;
        if(config.getWebhook().isStatisticsEnableCountryDetection() && ip != null){
            countryISO = getCountryISO(ip);
        }
        webHook.sendUsageData(new UsageData(validatorId, domain, api, validationType, result, countryISO));
    }

    	/**
	 * Method that extracts the HttpServletRequest from the REST and WEB requests
	*/
	protected HttpServletRequest getHttpRequest(JoinPoint joinPoint){
		HttpServletRequest request = null;
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		Class[] paramTypes = signature.getParameterTypes();
		for(int i = 0; i < paramTypes.length; i++){
			if(paramTypes[i] == HttpServletRequest.class){
				Object[] args = joinPoint.getArgs();
				return (HttpServletRequest)args[i];
			}
		}
		logger.warn("Unexpected execution point reached: HttpServletRequest object not found among API arguments.");
		return request;
	}
    
}
