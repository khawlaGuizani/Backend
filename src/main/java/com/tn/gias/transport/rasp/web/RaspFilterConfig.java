package com.tn.gias.transport.rasp.web;

import com.tn.gias.transport.rasp.RaspProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers RaspHttpFilter at the very front of the servlet filter chain —
 * ahead of Spring Security's filter chain — so attacks are intercepted
 * before authentication/authorization and before any business logic runs.
 */
@Configuration
public class RaspFilterConfig {

    @Bean
    public FilterRegistrationBean<RaspHttpFilter> raspFilterRegistration(RaspHttpFilter filter,
                                                                          RaspProperties properties) {
        FilterRegistrationBean<RaspHttpFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setEnabled(properties.isEnabled());
        registration.setName("raspHttpFilter");
        return registration;
    }
}
