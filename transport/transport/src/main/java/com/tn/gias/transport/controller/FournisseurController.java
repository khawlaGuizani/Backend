package com.tn.gias.transport.controller;
import com.tn.gias.transport.entity.Fournisseur;
import com.tn.gias.transport.service.FournisseurService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fournisseurs")
public class FournisseurController {
    private final FournisseurService service;

    public FournisseurController(FournisseurService service) {
        this.service = service;
    }

    @PostMapping
    public Fournisseur create(@RequestBody Fournisseur f) {
        return service.save(f);
    }

    @GetMapping
    public List<Fournisseur> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Fournisseur getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Fournisseur update(@PathVariable Long id, @RequestBody Fournisseur f) {
        return service.update(id, f);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
