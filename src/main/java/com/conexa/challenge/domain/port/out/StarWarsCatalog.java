package com.conexa.challenge.domain.port.out;

import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.model.StarWarsResource;
import java.util.List;
import java.util.Optional;

public interface StarWarsCatalog<T extends StarWarsResource> {

    PageResult<T> findPage(int page, int size);

    Optional<T> findById(int id);

    List<T> findByName(String name);
}
