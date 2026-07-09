package com.tn.gias.transport.controller;
import com.tn.gias.transport.entity.Site;
import com.tn.gias.transport.service.SiteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {
    private final SiteService service;

    public SiteController(SiteService service) {
        this.service = service;
    }

    @PostMapping
    public Site create(@RequestBody Site s) {
        return service.save(s);
    }

    @GetMapping
    public List<Site> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Site getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Site update(@PathVariable Long id, @RequestBody Site s) {
        return service.update(id, s);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
