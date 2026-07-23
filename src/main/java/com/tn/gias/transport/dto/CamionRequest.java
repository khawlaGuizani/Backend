package com.tn.gias.transport.dto;
import lombok.Data;

@Data
public class CamionRequest {
    //les attributs de la classe CamionRequest
    private String immatriculation;
    private boolean disponible;
    private double capaciteReelle;
    //private int annee;

    private Long typeCamionId;
}
