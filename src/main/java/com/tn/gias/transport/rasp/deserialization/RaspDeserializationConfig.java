package com.tn.gias.transport.rasp.deserialization;

import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.logging.RaspLogger;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.ObjectInputFilter;

/**
 * Layer 3 of RASP: installs a JVM-wide {@link ObjectInputFilter} (JEP 290,
 * Java 9+) that blocks known Java-deserialization gadget-chain classes
 * (Commons Collections functors, Groovy runtime, Xalan, BeanShell, RMI
 * remote objects, ...) and enforces sane depth/array/byte limits on any
 * native Java serialization stream in this process — protecting against
 * CVE-class deserialization RCE regardless of which code path in the app
 * ends up calling {@code new ObjectInputStream(...)}.
 */
@Configuration
public class RaspDeserializationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RaspDeserializationConfig.class);

    /**
     * JEP-290 pattern: explicit rejects (!) for well-known RCE gadget
     * classes, sane resource limits, then allow everything else — this app
     * does not rely on native Java serialization, but a blanket allow keeps
     * this filter safe to enable even if some dependency uses it internally.
     */
    private static final String FILTER_PATTERN = String.join(";",
            "!org.apache.commons.collections.functors.*",
            "!org.apache.commons.collections4.functors.*",
            "!org.codehaus.groovy.runtime.**",
            "!org.apache.xalan.**",
            "!com.sun.org.apache.xalan.**",
            "!bsh.**",
            "!org.springframework.beans.factory.ObjectFactory",
            "!java.rmi.server.RemoteObject",
            "!java.rmi.server.UnicastRemoteObject",
            "!com.sun.rowset.JdbcRowSetImpl",
            "maxdepth=10",
            "maxarray=100000",
            "maxrefs=100000",
            "maxbytes=10485760",
            "*"
    );

    private final RaspProperties properties;
    private final RaspLogger raspLogger;

    public RaspDeserializationConfig(RaspProperties properties, RaspLogger raspLogger) {
        this.properties = properties;
        this.raspLogger = raspLogger;
    }

    @PostConstruct
    public void installGlobalDeserializationFilter() {
        if (!properties.isEnabled()) {
            LOG.info("RASP disabled — skipping global ObjectInputFilter installation.");
            return;
        }
        if (ObjectInputFilter.Config.getSerialFilter() != null) {
            LOG.info("RASP: a JVM-wide ObjectInputFilter is already installed, leaving it in place.");
            return;
        }
        ObjectInputFilter patternFilter = ObjectInputFilter.Config.createFilter(FILTER_PATTERN);
        ObjectInputFilter.Config.setSerialFilter(new RaspObjectInputFilter(patternFilter, raspLogger, properties));
        LOG.info("RASP: global deserialization ObjectInputFilter installed (mode={}).", properties.getMode());
    }
}
