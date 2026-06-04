package com.tn.gias.transport.service;
import com.tn.gias.transport.dto.CamionRequest;
import com.tn.gias.transport.entity.Camion;
import com.tn.gias.transport.entity.TypeCamion;
import com.tn.gias.transport.repository.CamionRepository;
import com.tn.gias.transport.repository.TypeCamionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service

public class CamionService {

    private final CamionRepository repository;
    private final TypeCamionRepository typeCamionRepository;

    public CamionService(CamionRepository repository,
                         TypeCamionRepository typeCamionRepository) {
        this.repository = repository;
        this.typeCamionRepository = typeCamionRepository;
    }

    public Camion save(CamionRequest request) {

        TypeCamion type = typeCamionRepository.findById(request.getTypeCamionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TypeCamion introuvable"));

        Camion c = new Camion();
        c.setImmatriculation(request.getImmatriculation());
        c.setDisponible(request.isDisponible());
        c.setCapaciteReelle(request.getCapaciteReelle());
        c.setAnnee(request.getAnnee());
        c.setTypeCamion(type);

        return repository.save(c);
    }
    // ✅ AJOUTER ÇA
    public List<Camion> getAll() {
        return repository.findAll();
    }

    public Camion getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));
    }

    public Camion update(Long id, CamionRequest request) {
        Camion existing = getById(id);

        TypeCamion type = typeCamionRepository.findById(request.getTypeCamionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TypeCamion introuvable"));

        existing.setImmatriculation(request.getImmatriculation());
        existing.setDisponible(request.isDisponible());
        existing.setCapaciteReelle(request.getCapaciteReelle());
        existing.setAnnee(request.getAnnee());
        existing.setTypeCamion(type);

        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable");
        }
        repository.deleteById(id);
    }
}
