package com.tn.gias.transport.rasp;

/**
 * Operating mode of the RASP layer.
 *
 * BLOCK  - malicious requests/operations are rejected (HTTP 403 / exception).
 * DETECT - malicious requests/operations are only logged, never rejected.
 *          Useful to roll out new detection rules without risking false-
 *          positive outages ("shadow mode").
 */
public enum RaspMode {
    BLOCK,
    DETECT
}
