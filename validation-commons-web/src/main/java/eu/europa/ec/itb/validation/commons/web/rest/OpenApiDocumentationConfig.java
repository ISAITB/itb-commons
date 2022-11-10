package eu.europa.ec.itb.validation.commons.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the validator's REST API documentation (based on Swagger/OpenAPI).
 */
@Configuration
public class OpenApiDocumentationConfig {

    @Value("${validator.docs.licence.description}")
    private String licenceDescription;
    @Value("${validator.docs.licence.url}")
    private String licenceUrl;
    @Value("${validator.docs.version}")
    private String restApiVersion;
    @Value("${validator.docs.title}")
    private String restApiTitle;
    @Value("${validator.docs.description}")
    private String restApiDescription;

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title(restApiTitle)
                        .description(restApiDescription)
                        .version(restApiVersion)
                        .license(new License().name(licenceDescription).url(licenceUrl))
        );
    }

}
