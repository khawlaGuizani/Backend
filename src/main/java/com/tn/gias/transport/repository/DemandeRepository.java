package com.tn.gias.transport.repository;

import com.tn.gias.transport.entity.Demande;
import com.tn.gias.transport.entity.StatutDemande;
import com.tn.gias.transport.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findByStatut(StatutDemande statut);
    List<Demande> findByDemandeur(Utilisateur utilisateur);

}
