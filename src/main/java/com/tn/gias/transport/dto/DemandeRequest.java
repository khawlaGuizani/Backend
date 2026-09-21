package com.tn.gias.transport.dto;

import lombok.Data;
import java.util.List;

@Data
public class DemandeRequest {

    private String libelle;
    private double capacite;
    private String typeMouvement;
    private String descriptionMouvement;

    private Long siteDepartId;
    private Long siteArriveeId;
    ///private Long demandeurId;
    private Long fournisseurId;
    private Long camionId;

    private List<LigneDemandeRequest> lignes;
}
