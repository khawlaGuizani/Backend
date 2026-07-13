package com.tn.gias.transport.entity;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;
    private double capacite;

    private LocalDateTime dateDemande;
    private LocalDateTime dateValidation;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @Enumerated(EnumType.STRING)
    private TypeMouvement typeMouvement;

    // 🔗 relations

    @ManyToOne
    private Site siteDepart;

    @ManyToOne
    private Site siteArrivee;

    @ManyToOne
    private Utilisateur demandeur;

    @ManyToOne
    private Fournisseur fournisseur;

    @ManyToOne
    private Camion camion;



    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<LigneDemande> lignes;


}
