package com.tn.gias.transport.controller;

import com.tn.gias.transport.dto.ChangePasswordRequest;
import com.tn.gias.transport.entity.Utilisateur;
import com.tn.gias.transport.service.UtilisateurService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurController {

    private final UtilisateurService service;

    public UtilisateurController(UtilisateurService service) {
        this.service = service;
    }

    @PostMapping
    public Utilisateur create(@RequestBody Utilisateur u) {
        return service.save(u);
    }

    @GetMapping
    public List<Utilisateur> getAll() {
        return service.getAll();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}")
    public Utilisateur update(@PathVariable Long id, @RequestBody Utilisateur u) {
        u.setId(id);
        return service.save(u);
    }

    @PutMapping("/{id}/mot-de-passe")
    public Utilisateur changePassword(@PathVariable Long id, @RequestBody ChangePasswordRequest request) {
        return service.adminChangePassword(id, request.getNouveauMotDePasse());
    }



}
