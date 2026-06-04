package com.tn.gias.transport.controller;
import com.tn.gias.transport.entity.Article;
import com.tn.gias.transport.service.ArticleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Article create(@RequestBody Article a) {
        return service.save(a);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEMANDEUR')")
    @GetMapping
    public List<Article> getAll() {
        return service.getAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEMANDEUR')")
    @GetMapping("/{id}")
    public Article getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Article update(@PathVariable Long id, @RequestBody Article a) {
        return service.update(id, a);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
