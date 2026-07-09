package com.tn.gias.transport.service;
import com.tn.gias.transport.entity.Article;
import com.tn.gias.transport.repository.ArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    public Article save(Article a) {
        return repository.save(a);
    }

    public List<Article> getAll() {
        return repository.findAll();
    }

    public Article getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable"));
    }

    public Article update(Long id, Article input) {
        Article existing = getById(id);
        existing.setCodeArticle(input.getCodeArticle());
        existing.setUnit(input.getUnit());
        existing.setQuantite(input.getQuantite());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable");
        }
        repository.deleteById(id);
    }

}
