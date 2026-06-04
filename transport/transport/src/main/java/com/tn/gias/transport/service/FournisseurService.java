package com.tn.gias.transport.service;
import com.tn.gias.transport.entity.Fournisseur;
import com.tn.gias.transport.repository.FournisseurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FournisseurService {
    private final FournisseurRepository repository;

    public FournisseurService(FournisseurRepository repository) {
        this.repository = repository;
    }

    public Fournisseur save(Fournisseur f) {
        return repository.save(f);
    }

    public List<Fournisseur> getAll() {
        return repository.findAll();
    }

    public Fournisseur getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable"));
    }

    public Fournisseur update(Long id, Fournisseur input) {
        Fournisseur existing = getById(id);
        existing.setNom(input.getNom());
        existing.setContact(input.getContact());
        existing.setEmail(input.getEmail());
        existing.setActif(input.isActif());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable");
        }
        repository.deleteById(id);
    }
}
