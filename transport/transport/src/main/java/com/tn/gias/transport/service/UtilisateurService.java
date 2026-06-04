package com.tn.gias.transport.service;

import com.tn.gias.transport.entity.Utilisateur;
import com.tn.gias.transport.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Utilisateur u = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return User.builder()
                .username(u.getEmail())
                .password(u.getMotDePasse())
                .roles(u.getRole().name())
                .build();
    }

    public Utilisateur findByEmailAndPassword(String email, String motDePasse) {
        Utilisateur utilisateur = repository.findByEmail(email).orElse(null);
        if (utilisateur == null) {
            return null;
        }

        return passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse()) ? utilisateur : null;
    }

    public Utilisateur save(Utilisateur u) {
        if (u.getId() != null) {
            Utilisateur existing = getById(u.getId());
            if (u.getMotDePasse() == null || u.getMotDePasse().isBlank()) {
                u.setMotDePasse(existing.getMotDePasse());
            } else {
                u.setMotDePasse(passwordEncoder.encode(u.getMotDePasse()));
            }
        } else if (u.getMotDePasse() != null && !u.getMotDePasse().isBlank()) {
            u.setMotDePasse(passwordEncoder.encode(u.getMotDePasse()));
        }

        return repository.save(u);
    }

    public Utilisateur adminChangePassword(Long id, String nouveauMotDePasse) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.isBlank()) {
            throw new RuntimeException("Le mot de passe est obligatoire");
        }

        Utilisateur utilisateur = getById(id);
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        return repository.save(utilisateur);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Utilisateur getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Utilisateur findByEmail(String email) {
        return repository.findByEmail(email).orElse(null);
    }

    public List<Utilisateur> getAll() {
        return repository.findAll();
    }
}
