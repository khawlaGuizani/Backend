package com.tn.gias.transport.rasp.ssrf;

import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.core.RaspGuard;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Layer for SSRF protection: validates outbound HTTP call targets against
 * loopback/link-local/private (RFC1918) address ranges and the well-known
 * cloud metadata endpoint, before the request is allowed to leave the
 * application. Wired automatically into every RestTemplate via
 * {@link RaspRestTemplateInterceptor} / {@link RaspRestTemplateConfig}, and
 * available as {@link #checkTarget(String)} for manual use (WebClient
 * filters, raw HttpClient calls, ...).
 */
@Component
public class RaspSsrfGuard {

    private final RaspGuard guard;
    private final RaspProperties properties;

    public RaspSsrfGuard(RaspGuard guard, RaspProperties properties) {
        this.guard = guard;
        this.properties = properties;
    }

    public void checkTarget(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return;
        }
        checkTarget(uri.getHost());
    }

    public void checkTarget(String host) {
        if (properties.getSsrf().getAllowedHosts().contains(host)) {
            return;
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return; // cannot resolve — let the HTTP client fail naturally
        }

        if (isForbidden(address)) {
            guard.reportCustom("SSRF", "SSRF", "CRITICAL", "internal-address-blocked",
                    "Outbound request to internal/loopback/link-local address blocked: "
                            + host + " (" + address.getHostAddress() + ")");
        }
    }

    private boolean isForbidden(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isLinkLocalAddress()   // covers 169.254.0.0/16 -> cloud metadata endpoint
                || address.isSiteLocalAddress()   // covers 10/8, 172.16/12, 192.168/16
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();
    }
}
