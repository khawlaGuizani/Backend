package com.tn.gias.transport.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeSite;
    private String libelle;
    private String adresse;
    private String ville;
    private boolean actif;
}
