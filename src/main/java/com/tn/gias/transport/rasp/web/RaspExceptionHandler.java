package com.tn.gias.transport.rasp.web;

import com.tn.gias.transport.rasp.core.RaspSecurityException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Defense-in-depth: RASP hooks that fire deep inside the call stack (JDBC
 * guard, deserialization filter) throw RaspSecurityException. RaspHttpFilter
 * already catches it for the top-level request, but this advice ensures
 * that, whatever handler resolution path Spring MVC takes, the exception is
 * always surfaced as a clean HTTP 403 JSON body instead of a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RaspExceptionHandler {

    @ExceptionHandler(RaspSecurityException.class)
    public ResponseEntity<Map<String, Object>> handle(RaspSecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Forbidden",
                "message", "Request blocked by RASP",
                "attackType", e.getAttackType(),
                "status", 403
        ));
    }
}
