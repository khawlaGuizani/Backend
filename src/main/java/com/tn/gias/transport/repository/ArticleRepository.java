package com.tn.gias.transport.repository;

import com.tn.gias.transport.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
