package eu.europa.ec.itb.validation.commons.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

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
    @Value("${validator.docs.server.url:#{null}}")
    private String serverUrls;

    @Bean
    public OpenAPI api() {
        var openApi = new OpenAPI()
                .info(new Info()
                        .title(restApiTitle)
                        .description(restApiDescription)
                        .version(restApiVersion)
                        .license(new License().name(licenceDescription).url(licenceUrl))
        );
        if (StringUtils.isNotBlank(serverUrls)) {
            openApi = openApi.servers(Arrays.stream(StringUtils.split(serverUrls.trim(),','))
                .filter(StringUtils::isNotBlank)
                .map(url -> {
                    var server = new Server();
                    server.setUrl(url.trim());
                    server.setDescription("Generated server url");
                    return server;
                }).toList());
        }
        return openApi;
    }

}
