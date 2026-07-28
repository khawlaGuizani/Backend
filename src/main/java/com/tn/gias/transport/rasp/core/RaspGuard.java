package com.tn.gias.transport.rasp.core;

import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.logging.RaspEvent;
import com.tn.gias.transport.rasp.logging.RaspLogger;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Facade used by every RASP hook (HTTP filter, JDBC guard, SSRF interceptor,
 * deserialization filter, file upload validator, demo endpoints, ...).
 *
 * A single place decides: does this input match an attack signature, should
 * the event be logged, and — depending on {@link RaspProperties#getMode()} —
 * should the operation be blocked by throwing {@link RaspSecurityException}.
 */
@Component
public class RaspGuard {

    private final RaspProperties properties;
    private final RaspLogger logger;

    public RaspGuard(RaspProperties properties, RaspLogger logger) {
        this.properties = properties;
        this.logger = logger;
    }

    /**
     * @return true if an attack signature matched (regardless of block/detect mode)
     */
    public boolean check(String layer, String attackType, String severity,
                          Pattern pattern, String input, String ruleName) {
        if (!properties.isEnabled() || input == null) {
            return false;
        }
        if (!RaspPatterns.matches(pattern, input)) {
            return false;
        }

        RaspRequestContext ctx = RaspRequestContext.current();
        boolean blocking = properties.isBlocking();

        RaspEvent event = RaspEvent.builder()
                .layer(layer)
                .attackType(attackType)
                .severity(severity)
                .sourceIp(ctx.getSourceIp())
                .httpMethod(ctx.getHttpMethod())
                .uri(ctx.getUri())
                .matchedRule(ruleName)
                .payloadSnippet(input)
                .blocked(blocking)
                .mode(properties.getMode().name())
                .build();
        logger.log(event);

        if (blocking) {
            throw new RaspSecurityException(attackType,
                    "RASP blocked a " + attackType + " attempt (rule=" + ruleName + ")");
        }
        return true;
    }

    public void checkSql(String layer, String sql) {
        check(layer, "SQL_INJECTION", "CRITICAL", RaspPatterns.SQL_INJECTION, sql, "sql-injection-signature");
    }

    public void checkPathTraversal(String layer, String path) {
        check(layer, "PATH_TRAVERSAL", "HIGH", RaspPatterns.PATH_TRAVERSAL, path, "path-traversal-signature");
    }

    public void checkCommand(String layer, String command) {
        check(layer, "COMMAND_INJECTION", "CRITICAL", RaspPatterns.COMMAND_INJECTION, command, "command-injection-signature");
    }

    public void checkXxe(String layer, String xmlBody) {
        check(layer, "XXE", "HIGH", RaspPatterns.XXE, xmlBody, "xxe-signature");
    }

    public void checkLdap(String layer, String filter) {
        check(layer, "LDAP_INJECTION", "HIGH", RaspPatterns.LDAP_INJECTION, filter, "ldap-injection-signature");
    }

    public void checkExpression(String layer, String expression) {
        check(layer, "EXPRESSION_INJECTION", "CRITICAL", RaspPatterns.EXPRESSION_INJECTION, expression, "expression-injection-signature");
    }

    public void checkFileName(String layer, String filename) {
        check(layer, "DANGEROUS_FILE_UPLOAD", "HIGH", RaspPatterns.DANGEROUS_FILE_EXTENSION, filename, "dangerous-extension-signature");
    }

    /** Logs+optionally blocks an event whose detection logic lives outside RaspPatterns (e.g. SSRF IP-range checks). */
    public void reportCustom(String layer, String attackType, String severity, String matchedRule, String detail) {
        if (!properties.isEnabled()) {
            return;
        }
        RaspRequestContext ctx = RaspRequestContext.current();
        boolean blocking = properties.isBlocking();

        RaspEvent event = RaspEvent.builder()
                .layer(layer)
                .attackType(attackType)
                .severity(severity)
                .sourceIp(ctx.getSourceIp())
                .httpMethod(ctx.getHttpMethod())
                .uri(ctx.getUri())
                .matchedRule(matchedRule)
                .detail(detail)
                .blocked(blocking)
                .mode(properties.getMode().name())
                .build();
        logger.log(event);

        if (blocking) {
            throw new RaspSecurityException(attackType,
                    "RASP blocked a " + attackType + " attempt (rule=" + matchedRule + ")");
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }
}
