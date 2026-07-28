package com.tn.gias.transport.rasp.deserialization;

import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.logging.RaspEvent;
import com.tn.gias.transport.rasp.logging.RaspLogger;

import java.io.ObjectInputFilter;

/**
 * Wraps a JEP-290 pattern-based ObjectInputFilter to add RASP structured
 * logging. Returning REJECTED causes any ObjectInputStream in the JVM to
 * throw InvalidClassException for that class — this is how the JDK itself
 * enforces the block, so DETECT mode is honored by downgrading REJECTED to
 * ALLOWED rather than by catching an exception after the fact.
 */
class RaspObjectInputFilter implements ObjectInputFilter {

    private final ObjectInputFilter delegate;
    private final RaspLogger logger;
    private final RaspProperties properties;

    RaspObjectInputFilter(ObjectInputFilter delegate, RaspLogger logger, RaspProperties properties) {
        this.delegate = delegate;
        this.logger = logger;
        this.properties = properties;
    }

    @Override
    public Status checkInput(FilterInfo info) {
        Status status = delegate.checkInput(info);
        if (status != Status.REJECTED) {
            return status;
        }

        String className = info.serialClass() != null ? info.serialClass().getName() : "unknown";
        boolean blocking = properties.isBlocking();

        RaspEvent event = RaspEvent.builder()
                .layer("DESERIALIZATION")
                .attackType("INSECURE_DESERIALIZATION")
                .severity("CRITICAL")
                .matchedRule("object-input-filter-blacklist")
                .detail("Blocked deserialization of class: " + className)
                .blocked(blocking)
                .mode(properties.getMode().name())
                .build();
        logger.log(event);

        return blocking ? Status.REJECTED : Status.ALLOWED;
    }
}
