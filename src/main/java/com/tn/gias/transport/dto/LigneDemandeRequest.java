package com.tn.gias.transport.dto;

import lombok.Data;

@Data
public class LigneDemandeRequest {

    private Long articleId;
    private int quantite;
    private String unite;
    private String type; // ENTREE / SORTIE
    private String description;

}
