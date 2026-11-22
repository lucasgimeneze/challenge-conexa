package com.conexa.challenge.infrastructure.config;

import com.conexa.challenge.application.service.StarWarsQueryService;
import com.conexa.challenge.domain.model.Film;
import com.conexa.challenge.domain.model.Person;
import com.conexa.challenge.domain.model.Starship;
import com.conexa.challenge.domain.model.Vehicle;
import com.conexa.challenge.infrastructure.adapter.out.swapi.FilmsSwapiCatalog;
import com.conexa.challenge.infrastructure.adapter.out.swapi.RemoteSwapiCatalog;
import com.conexa.challenge.infrastructure.adapter.out.swapi.SwapiClient;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiPersonProperties;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiStarshipProperties;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiVehicleProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class StarWarsConfig {

    @Bean
    StarWarsQueryService<Person> peopleService(SwapiClient client) {
        return new StarWarsQueryService<>(new RemoteSwapiCatalog<>(client, "people", SwapiPersonProperties.class));
    }

    @Bean
    StarWarsQueryService<Starship> starshipsService(SwapiClient client) {
        return new StarWarsQueryService<>(new RemoteSwapiCatalog<>(client, "starships", SwapiStarshipProperties.class));
    }

    @Bean
    StarWarsQueryService<Vehicle> vehiclesService(SwapiClient client) {
        return new StarWarsQueryService<>(new RemoteSwapiCatalog<>(client, "vehicles", SwapiVehicleProperties.class));
    }

    @Bean
    StarWarsQueryService<Film> filmsService(SwapiClient client) {
        return new StarWarsQueryService<>(new FilmsSwapiCatalog(client));
    }
}
