package com.tn.gias.transport.controller;
import com.tn.gias.transport.entity.TypeCamion;
import com.tn.gias.transport.service.TypeCamionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/type-camions")
public class TypeCamionController {

    private final TypeCamionService service;

    public TypeCamionController(TypeCamionService service) {
        this.service = service;
    }

    @PostMapping
    public TypeCamion create(@RequestBody TypeCamion t) {
        return service.save(t);
    }

    @GetMapping
    public List<TypeCamion> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public TypeCamion getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public TypeCamion update(@PathVariable Long id, @RequestBody TypeCamion t) {
        return service.update(id, t);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
