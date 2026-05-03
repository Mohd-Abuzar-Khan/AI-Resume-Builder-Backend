package com.resumade.template.repository;

import com.resumade.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Integer> {
    List<Template> findByIsActiveTrue();
    List<Template> findByIsActiveTrueAndIsPremiumFalse();
    List<Template> findByIsActiveTrueAndIsPremiumTrue();
    List<Template> findByIsActiveTrueAndCategory(Template.Category category);
    List<Template> findByIsActiveTrueOrderByUsageCountDesc();
}
