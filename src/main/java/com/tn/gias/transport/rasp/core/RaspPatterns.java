package com.tn.gias.transport.rasp.core;

import java.util.regex.Pattern;

/**
 * Central library of attack-signature heuristics used by every RASP hook
 * (HTTP filter, JDBC guard, demo endpoints, ...). Pattern-based detection is
 * inherently probabilistic — the goal here is to catch the overwhelming
 * majority of real-world exploit payloads with a low false-positive rate,
 * as a defense-in-depth layer behind proper input validation and
 * parameterized queries, not a replacement for them.
 */
public final class RaspPatterns {

    private RaspPatterns() {
    }

    // --- SQL Injection ---------------------------------------------------
    // Deliberately NOT matching bare DML/DDL keywords (select/insert/update/
    // alter/...): every legitimate Hibernate/JPA-generated statement contains
    // "select ... from", and schema DDL from spring.jpa.hibernate.ddl-auto
    // legitimately contains "alter table" — matching those would block 100%
    // of normal traffic and even crash startup. Instead this targets
    // injection-specific ANOMALIES that never appear in parameterized,
    // driver-generated SQL: tautologies, stacked queries, UNION-based
    // exfiltration, comment-terminated injection and blind-SQLi primitives.
    public static final Pattern SQL_INJECTION = Pattern.compile(
            "('\\s*or\\s*'?\\d+'?\\s*=\\s*'?\\d+)" +                       // ' or '1'='1
                    "|(\\bor\\b\\s+\\d+\\s*=\\s*\\d+)" +                   // or 1=1
                    "|(\\bunion\\s+select\\b)" +                          // UNION SELECT ...
                    "|(;\\s*(drop|delete|insert|update|alter|exec|create)\\b)" + // stacked query
                    "|('\\s*(--|#))" +                                    // quote-terminated comment injection
                    "|(\\bxp_cmdshell\\b)" +                              // SQL Server command exec proc
                    "|(\\bsp_executesql\\b)" +
                    "|(\\bwaitfor\\s+delay\\b)" +                         // blind SQLi timing primitive
                    "|(\\bbenchmark\\s*\\()" +                            // MySQL blind SQLi timing primitive
                    "|(\\bsleep\\s*\\(\\s*\\d)",                          // sleep(N) timing primitive
            Pattern.CASE_INSENSITIVE);

    // --- Path Traversal / LFI -----------------------------------------
    public static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "(\\.\\.[/\\\\])|(%2e%2e[/\\\\])|(%252e%252e)|(\\.\\.%2f)|(/etc/passwd)|(/etc/shadow)|" +
                    "(win\\.ini)|(boot\\.ini)|(\\\\windows\\\\system32)", Pattern.CASE_INSENSITIVE);

    // --- OS Command Injection ------------------------------------------
    public static final Pattern COMMAND_INJECTION = Pattern.compile(
            "([;&|`]\\s*(cat|ls|dir|whoami|id|uname|wget|curl|nc|netcat|ping|nslookup|rm|chmod|chown|powershell|cmd\\.exe)\\b)" +
                    "|(\\$\\([^)]+\\))|(`[^`]+`)|(\\|\\|\\s*\\w+)|(&&\\s*\\w+)", Pattern.CASE_INSENSITIVE);

    // --- XXE marker (raw body inspection) -------------------------------
    public static final Pattern XXE = Pattern.compile(
            "(<!DOCTYPE[^>]+SYSTEM)|(<!ENTITY)|(SYSTEM\\s+[\"'](file|http|ftp|jar|netdoc):)",
            Pattern.CASE_INSENSITIVE);

    // --- LDAP Injection --------------------------------------------------
    public static final Pattern LDAP_INJECTION = Pattern.compile(
            "(\\*\\)(\\(|\\|))|(\\)\\(\\|)|(\\(\\|\\()|(admin\\*)|(\\)\\(cn=\\*)", Pattern.CASE_INSENSITIVE);

    // --- Expression Injection (SpEL / OGNL) -------------------------------
    public static final Pattern EXPRESSION_INJECTION = Pattern.compile(
            "(T\\(\\s*java\\.lang\\.(Runtime|ProcessBuilder|System))|(#\\{.*\\})|(\\$\\{.*(runtime|exec).*})|" +
                    "(new\\s+java\\.lang\\.ProcessBuilder)|(getRuntime\\(\\)\\.exec)", Pattern.CASE_INSENSITIVE);

    // --- Dangerous file upload extensions ---------------------------------
    public static final Pattern DANGEROUS_FILE_EXTENSION = Pattern.compile(
            "(?i)\\.(jsp|jspx|php\\d?|phtml|phar|exe|bat|cmd|sh|ps1|jar|war|dll|msi|scr|vbs|py|rb|asp|aspx|cgi|htaccess|reg)$"
    );

    public static boolean matches(Pattern pattern, String input) {
        return input != null && pattern.matcher(input).find();
    }
}
