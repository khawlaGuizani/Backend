package com.tn.gias.transport.repository;

import com.tn.gias.transport.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur,Long> {

    Optional<Utilisateur> findByEmail(String email);

    Utilisateur findByEmailAndMotDePasse(String email, String motDePasse);
}
