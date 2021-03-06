package eu.europa.ec.itb.validation.commons.war.config;

import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Validator web application security configuration.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig <Y extends WebDomainConfig, X extends WebDomainConfigCache<Y>> extends WebSecurityConfigurerAdapter {

    @Autowired
    private X domainConfigCache;

    /**
     * Check to see whether this validator includes a validator configuration that supports embedding.
     *
     * @return The check result.
     */
    private boolean hasEmbeddableValidator() {
        for (var config: domainConfigCache.getAllDomainConfigurations()) {
            if (config.isSupportUserInterfaceEmbedding()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enable CORS and disable CSRF checks.
     *
     * @see WebSecurityConfigurerAdapter#configure(HttpSecurity)
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // Enable all CORS requests (see corsFilter bean).
                .cors()
        .and()
                // Disabling as this has issues when running behind a reverse proxy for a stateless app.
                .csrf().disable();
        if (hasEmbeddableValidator()) {
            // Disabling X-Frame-Options by default to set it by domain (see HeaderFilter).
            http.headers().frameOptions().disable();
        } else {
            http.headers().frameOptions().deny();
        }
    }

    /**
     * Create a CORS filter to allow CORS requests.
     *
     * @return The filter bean.
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Empty configuration as there is no authentication for validators.
     *
     * @param authManager The authentication manager.
     */
    @Override
    protected void configure(AuthenticationManagerBuilder authManager) {
        // No authentication.
    }

    /**
     * @see WebSecurityConfigurerAdapter#authenticationManagerBean()
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}