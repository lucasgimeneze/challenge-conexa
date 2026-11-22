package com.conexa.challenge.application.service;

import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.model.StarWarsResource;
import com.conexa.challenge.domain.port.out.StarWarsCatalog;
import java.util.List;

public class StarWarsQueryService<T extends StarWarsResource> {

    private final StarWarsCatalog<T> catalog;

    public StarWarsQueryService(StarWarsCatalog<T> catalog) {
        this.catalog = catalog;
    }

    public PageResult<T> find(Integer id, String name, int page, int size) {
        if (id != null) {
            return PageResult.of(findByIdAndName(id, name), page, size);
        }
        if (name != null) {
            return PageResult.of(catalog.findByName(name), page, size);
        }
        return catalog.findPage(page, size);
    }

    private List<T> findByIdAndName(int id, String name) {
        return catalog.findById(id)
                .filter(resource -> name == null || resource.nameContains(name))
                .stream()
                .toList();
    }
}
