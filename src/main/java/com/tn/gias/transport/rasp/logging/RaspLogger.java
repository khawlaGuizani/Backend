package com.tn.gias.transport.rasp.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tn.gias.transport.rasp.RaspProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Writes RASP detection events as structured JSON Lines (NDJSON) — one JSON
 * object per line, one file per day — under {@code rasp.log-dir}. This
 * format is directly consumable by Filebeat/Logstash for ELK ingestion, or
 * by `jq`/`grep` for a quick local look.
 *
 * Also mirrors each event to the standard application logger so it shows up
 * in `docker logs` / console output during a live demo.
 */
@Component
public class RaspLogger {

    private static final Logger LOG = LoggerFactory.getLogger("RASP");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RaspProperties properties;

    public RaspLogger(RaspProperties properties) {
        this.properties = properties;
    }

    public synchronized void log(RaspEvent event) {
        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (IOException e) {
            LOG.error("RASP: failed to serialize event", e);
            return;
        }

        logToConsole(event, json);
        writeToFile(json);
    }

    private void logToConsole(RaspEvent event, String json) {
        if ("CRITICAL".equals(event.getSeverity()) || "HIGH".equals(event.getSeverity())) {
            LOG.warn(json);
        } else {
            LOG.info(json);
        }
    }

    private void writeToFile(String json) {
        try {
            Path dir = Path.of(properties.getLogDir());
            Files.createDirectories(dir);
            Path file = dir.resolve("rasp-events-" + LocalDate.now().format(DAY) + ".log");
            Files.writeString(file, json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("RASP: failed to write event to {}", properties.getLogDir(), e);
        }
    }
}
