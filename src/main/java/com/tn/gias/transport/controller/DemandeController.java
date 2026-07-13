package com.tn.gias.transport.controller;

import com.tn.gias.transport.dto.DemandeRequest;
import com.tn.gias.transport.entity.Demande;
import com.tn.gias.transport.entity.StatutDemande;
import com.tn.gias.transport.service.DemandeService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeService service;

    public DemandeController(DemandeService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('DEMANDEUR')")
    @PostMapping
    public Demande create(@RequestBody DemandeRequest request) {
        return service.save(request);
    }

    @PreAuthorize("hasRole('DEMANDEUR')")
    @GetMapping("/mes-demandes")
    public List<Demande> mesDemandes() {
        return service.getMesDemandes();
    }

    @PreAuthorize("hasAnyRole('VALIDATEUR','ADMIN')")
    @GetMapping("/en-attente")
    public List<Demande> enAttente() {
        return service.getByStatut(StatutDemande.EN_ATTENTE);
    }

    @PreAuthorize("hasAnyRole('VALIDATEUR','ADMIN')")
    @PutMapping("/{id}/valider")
    public Demande valider(@PathVariable Long id) {
        return service.valider(id);
    }

    @PreAuthorize("hasAnyRole('VALIDATEUR','ADMIN')")
    @PutMapping(value = "/{id}/rejeter", consumes = "application/json")
    public Demande rejeter(@PathVariable Long id,
                           @RequestBody Map<String, String> body) {

        if (body == null || body.get("motif") == null || body.get("motif").isEmpty()) {
            throw new RuntimeException("Motif obligatoire");
        }

        String motif = body.get("motif");

        return service.refuser(id, motif);
    }
    @PreAuthorize("hasAnyRole('ADMIN','VALIDATEUR')")
    @GetMapping("/all")
    public List<Demande> getAll(
            @RequestParam(required = false) StatutDemande statut
    ) {
        if (statut != null) {
            return service.getByStatut(statut);
        }
        return service.getAll();
    }
    @PreAuthorize("hasAnyRole('VALIDATEUR','ADMIN')")

    @GetMapping("/{id}/csv")
    public ResponseEntity<Resource> downloadCsv(@PathVariable Long id) throws Exception {

        File file = service.telechargerCsv(id);

        InputStreamResource resource =
                new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + file.getName()
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
