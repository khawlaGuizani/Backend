package com.tn.gias.transport.rasp.ssrf;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

class RaspRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final RaspSsrfGuard ssrfGuard;

    RaspRestTemplateInterceptor(RaspSsrfGuard ssrfGuard) {
        this.ssrfGuard = ssrfGuard;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ssrfGuard.checkTarget(request.getURI());
        return execution.execute(request, body);
    }
}
