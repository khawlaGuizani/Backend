package com.tn.gias.transport.rasp.core;

/**
 * Thread-local carrier propagating the current HTTP request's identity
 * (method, URI, source IP) down to deeper RASP hooks (JDBC guard,
 * deserialization filter, ...) that have no direct access to the
 * HttpServletRequest, since they run later on the same request thread.
 * Set by RaspHttpFilter at the start of each request and always cleared in
 * a finally block.
 */
public final class RaspRequestContext {

    private static final ThreadLocal<RaspRequestContext> CURRENT = new ThreadLocal<>();

    private final String httpMethod;
    private final String uri;
    private final String sourceIp;

    private RaspRequestContext(String httpMethod, String uri, String sourceIp) {
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.sourceIp = sourceIp;
    }

    public static void set(String httpMethod, String uri, String sourceIp) {
        CURRENT.set(new RaspRequestContext(httpMethod, uri, sourceIp));
    }

    public static RaspRequestContext current() {
        RaspRequestContext ctx = CURRENT.get();
        return ctx != null ? ctx : new RaspRequestContext(null, null, null);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getUri() {
        return uri;
    }

    public String getSourceIp() {
        return sourceIp;
    }
}
