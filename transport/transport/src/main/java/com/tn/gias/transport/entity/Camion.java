package com.tn.gias.transport.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String immatriculation;
    private boolean disponible;
    private double capaciteReelle;
    private int annee;



    @ManyToOne
    @JoinColumn(name = "type_camion_id")
    private TypeCamion typeCamion;

}
