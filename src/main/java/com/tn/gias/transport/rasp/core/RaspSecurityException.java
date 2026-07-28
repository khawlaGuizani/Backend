package com.tn.gias.transport.rasp.core;

/**
 * Thrown by a RASP hook when an attack is detected and the layer is running
 * in BLOCK mode. Handled centrally by RaspExceptionHandler and turned into
 * an HTTP 403 response.
 */
public class RaspSecurityException extends RuntimeException {

    private final String attackType;

    public RaspSecurityException(String attackType, String message) {
        super(message);
        this.attackType = attackType;
    }

    public String getAttackType() {
        return attackType;
    }
}
