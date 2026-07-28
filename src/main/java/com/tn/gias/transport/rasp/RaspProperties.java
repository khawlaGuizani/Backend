package com.tn.gias.transport.rasp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Central configuration for the RASP (Runtime Application Self-Protection)
 * layer. Bound from the {@code rasp.*} properties (application.properties,
 * environment variables, or Docker Compose env — see README-DEVSECOPS.md).
 */
@Component
@ConfigurationProperties(prefix = "rasp")
public class RaspProperties {

    /** Master switch. When false, every RASP hook becomes a no-op. */
    private boolean enabled = true;

    /** BLOCK (reject attacks) or DETECT (log only, "shadow mode"). */
    private RaspMode mode = RaspMode.BLOCK;

    /** Directory where structured JSON attack logs are written. */
    private String logDir = "./rasp-logs";

    private final Ssrf ssrf = new Ssrf();
    private final Demo demo = new Demo();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RaspMode getMode() {
        return mode;
    }

    public void setMode(RaspMode mode) {
        this.mode = mode;
    }

    public boolean isBlocking() {
        return enabled && mode == RaspMode.BLOCK;
    }

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public Ssrf getSsrf() {
        return ssrf;
    }

    public Demo getDemo() {
        return demo;
    }

    public static class Ssrf {
        /** Extra allow-listed hostnames/IPs, comma-separated, besides loopback exclusions. */
        private List<String> allowedHosts = List.of();

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
        }
    }

    public static class Demo {
        /**
         * Enables /api/rasp-demo/** — deliberately vulnerable endpoints used
         * ONLY to demonstrate that RASP intercepts attacks before they reach
         * business logic. Must stay false in any real deployment.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
