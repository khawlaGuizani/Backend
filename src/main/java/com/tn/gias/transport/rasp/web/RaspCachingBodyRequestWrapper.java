package com.tn.gias.transport.rasp.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Buffers the raw request body in memory so RASP can inspect it (SQLi/XXE/
 * command/expression signatures) and still hand an untouched, fully
 * re-readable stream down to the real Spring MVC argument resolvers
 * (@RequestBody, etc.).
 */
class RaspCachingBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    RaspCachingBodyRequestWrapper(HttpServletRequest request, byte[] cachedBody) {
        super(request);
        this.cachedBody = cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // not needed for synchronous processing
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String encoding = getCharacterEncoding() != null ? getCharacterEncoding() : StandardCharsets.UTF_8.name();
        return new BufferedReader(new InputStreamReader(getInputStream(), encoding));
    }

    String getCachedBodyAsString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }
}
