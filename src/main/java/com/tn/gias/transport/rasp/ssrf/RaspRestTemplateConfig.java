package com.tn.gias.transport.rasp.ssrf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Exposes an SSRF-guarded {@link RestTemplate} bean: every outbound call
 * made through it is checked by {@link RaspSsrfGuard} before the request
 * leaves the application. Autowire this bean (rather than `new
 * RestTemplate()`) anywhere the app needs to call another HTTP service.
 */
@Configuration
public class RaspRestTemplateConfig {

    @Bean
    public RestTemplate raspRestTemplate(RaspSsrfGuard ssrfGuard) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new RaspRestTemplateInterceptor(ssrfGuard));
        return restTemplate;
    }
}
