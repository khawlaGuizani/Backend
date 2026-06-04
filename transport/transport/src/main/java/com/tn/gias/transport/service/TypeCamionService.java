package com.tn.gias.transport.service;
import com.tn.gias.transport.entity.TypeCamion;
import com.tn.gias.transport.repository.TypeCamionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TypeCamionService {

    private final TypeCamionRepository repository;

    public TypeCamionService(TypeCamionRepository repository) {
        this.repository = repository;
    }

    public TypeCamion save(TypeCamion t) {
        return repository.save(t);
    }

    public List<TypeCamion> getAll() {
        return repository.findAll();
    }

    public TypeCamion getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TypeCamion introuvable"));
    }

    public TypeCamion update(Long id, TypeCamion input) {
        TypeCamion existing = getById(id);
        existing.setLibelle(input.getLibelle());
        existing.setCapaciteMax(input.getCapaciteMax());
        existing.setDescription(input.getDescription());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TypeCamion introuvable");
        }
        repository.deleteById(id);
    }
}
