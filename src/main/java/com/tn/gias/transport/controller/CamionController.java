package com.tn.gias.transport.controller;
import com.tn.gias.transport.dto.CamionRequest;
import com.tn.gias.transport.entity.Camion;
import com.tn.gias.transport.service.CamionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/camions")
public class CamionController {
    private final CamionService service;

    public CamionController(CamionService service) {
        this.service = service;
    }

    @PostMapping
    public Camion create(@RequestBody CamionRequest request) {
        return service.save(request);
    }

    @GetMapping
    public List<Camion> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Camion getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Camion update(@PathVariable Long id, @RequestBody CamionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
