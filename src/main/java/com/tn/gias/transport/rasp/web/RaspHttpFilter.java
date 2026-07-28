package com.tn.gias.transport.rasp.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.core.RaspGuard;
import com.tn.gias.transport.rasp.core.RaspRequestContext;
import com.tn.gias.transport.rasp.core.RaspSecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Layer 1 of RASP: inspects every inbound HTTP request — URI, query string,
 * form/query parameters, request body (JSON/XML/text, size-capped) and
 * uploaded file names — for SQL Injection, Path Traversal, Command
 * Injection, XXE markers, LDAP Injection, Expression Injection and
 * dangerous file uploads. Runs at the very front of the filter chain
 * (see RaspFilterConfig), before Spring Security and before any controller
 * code, so a detected attack never reaches business logic.
 */
@Component
public class RaspHttpFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RaspHttpFilter.class);
    private static final int MAX_BODY_INSPECTION_BYTES = 1_000_000; // 1MB cap

    private final RaspGuard guard;
    private final RaspProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RaspHttpFilter(RaspGuard guard, RaspProperties properties) {
        this.guard = guard;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String sourceIp = resolveClientIp(request);
        RaspRequestContext.set(request.getMethod(), request.getRequestURI(), sourceIp);

        try {
            HttpServletRequest effectiveRequest = inspect(request);
            chain.doFilter(effectiveRequest, response);
        } catch (RaspSecurityException e) {
            LOG.warn("RASP blocked request {} {} from {}: {}",
                    request.getMethod(), request.getRequestURI(), sourceIp, e.getMessage());
            writeBlockedResponse(response, e);
        } finally {
            RaspRequestContext.clear();
        }
    }

    private HttpServletRequest inspect(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String contentType = request.getContentType() == null ? "" : request.getContentType();

        guard.checkPathTraversal("HTTP", uri);
        if (queryString != null) {
            String decoded = safeUrlDecode(queryString);
            guard.checkSql("HTTP", decoded);
            guard.checkCommand("HTTP", decoded);
            guard.checkExpression("HTTP", decoded);
            guard.checkLdap("HTTP", decoded);
            guard.checkPathTraversal("HTTP", decoded);
        }

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            for (String value : entry.getValue()) {
                guard.checkSql("HTTP", value);
                guard.checkCommand("HTTP", value);
                guard.checkExpression("HTTP", value);
                guard.checkLdap("HTTP", value);
                guard.checkPathTraversal("HTTP", value);
            }
        }

        if (contentType.toLowerCase().startsWith("multipart/")) {
            inspectMultipart(request);
            return request;
        }

        if (isInspectableBody(contentType)) {
            return inspectBody(request);
        }

        return request;
    }

    private void inspectMultipart(HttpServletRequest request) {
        try {
            for (Part part : request.getParts()) {
                String filename = part.getSubmittedFileName();
                if (filename != null && !filename.isBlank()) {
                    guard.checkFileName("FILE_UPLOAD", filename);
                }
            }
        } catch (Exception e) {
            LOG.debug("RASP: could not enumerate multipart parts for inspection", e);
        }
    }

    private boolean isInspectableBody(String contentType) {
        String ct = contentType.toLowerCase();
        return ct.startsWith("application/json")
                || ct.startsWith("application/xml")
                || ct.startsWith("text/xml")
                || ct.startsWith("text/plain");
    }

    private HttpServletRequest inspectBody(HttpServletRequest request) throws IOException {
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        RaspCachingBodyRequestWrapper wrapped = new RaspCachingBodyRequestWrapper(request, body);

        if (body.length > 0 && body.length <= MAX_BODY_INSPECTION_BYTES) {
            String text = wrapped.getCachedBodyAsString();
            guard.checkXxe("HTTP", text);
            guard.checkSql("HTTP", text);
            guard.checkCommand("HTTP", text);
            guard.checkExpression("HTTP", text);
        }
        return wrapped;
    }

    private String safeUrlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeBlockedResponse(HttpServletResponse response, RaspSecurityException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = Map.of(
                "error", "Forbidden",
                "message", "Request blocked by RASP",
                "attackType", e.getAttackType(),
                "status", 403
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Never inspect the actuator health probe used by Docker/orchestrators.
        return request.getRequestURI().startsWith("/actuator/health");
    }
}
