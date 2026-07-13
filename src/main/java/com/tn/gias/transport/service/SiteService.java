package com.tn.gias.transport.service;
import com.tn.gias.transport.entity.Site;
import com.tn.gias.transport.repository.SiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository repository;

    public SiteService(SiteRepository repository) {
        this.repository = repository;
    }

    public Site save(Site s) {
        return repository.save(s);
    }

    public List<Site> getAll() {
        return repository.findAll();
    }

    public Site getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site introuvable"));
    }

    public Site update(Long id, Site input) {
        Site existing = getById(id);
        existing.setCodeSite(input.getCodeSite());
        existing.setLibelle(input.getLibelle());
        existing.setAdresse(input.getAdresse());
        existing.setVille(input.getVille());
        existing.setActif(input.isActif());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Site introuvable");
        }
        repository.deleteById(id);
    }
}
