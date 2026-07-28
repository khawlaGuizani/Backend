package com.tn.gias.transport.rasp.logging;

import java.time.Instant;

/**
 * A single RASP detection event, serialized as one JSON line (NDJSON) per
 * event so logs are trivially ingestible by Filebeat/Logstash into ELK, or
 * by any other log shipper.
 */
public class RaspEvent {

    private final Instant timestamp = Instant.now();
    private String layer;          // HTTP, JDBC, DESERIALIZATION, SSRF, XXE, FILE_UPLOAD, LDAP, EXPRESSION
    private String attackType;     // SQL_INJECTION, PATH_TRAVERSAL, COMMAND_INJECTION, ...
    private String severity;       // LOW, MEDIUM, HIGH, CRITICAL
    private String sourceIp;
    private String httpMethod;
    private String uri;
    private String matchedRule;
    private String payloadSnippet;
    private boolean blocked;
    private String mode;           // BLOCK / DETECT
    private String detail;

    public static RaspEvent.Builder builder() {
        return new Builder();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getLayer() {
        return layer;
    }

    public String getAttackType() {
        return attackType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getUri() {
        return uri;
    }

    public String getMatchedRule() {
        return matchedRule;
    }

    public String getPayloadSnippet() {
        return payloadSnippet;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getMode() {
        return mode;
    }

    public String getDetail() {
        return detail;
    }

    public static final class Builder {
        private final RaspEvent event = new RaspEvent();

        public Builder layer(String layer) {
            event.layer = layer;
            return this;
        }

        public Builder attackType(String attackType) {
            event.attackType = attackType;
            return this;
        }

        public Builder severity(String severity) {
            event.severity = severity;
            return this;
        }

        public Builder sourceIp(String sourceIp) {
            event.sourceIp = sourceIp;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            event.httpMethod = httpMethod;
            return this;
        }

        public Builder uri(String uri) {
            event.uri = uri;
            return this;
        }

        public Builder matchedRule(String matchedRule) {
            event.matchedRule = matchedRule;
            return this;
        }

        public Builder payloadSnippet(String payloadSnippet) {
            event.payloadSnippet = payloadSnippet == null
                    ? null
                    : payloadSnippet.substring(0, Math.min(200, payloadSnippet.length()));
            return this;
        }

        public Builder blocked(boolean blocked) {
            event.blocked = blocked;
            return this;
        }

        public Builder mode(String mode) {
            event.mode = mode;
            return this;
        }

        public Builder detail(String detail) {
            event.detail = detail;
            return this;
        }

        public RaspEvent build() {
            return event;
        }
    }
}
