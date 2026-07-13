package com.tn.gias.transport.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int quantite;
    private String unite;

    @ManyToOne
    private Article article;

    @Enumerated(EnumType.STRING)
    private TypeMouvement type;
    private String description;

    @ManyToOne
    @JsonBackReference
    private Demande demande;
}
