package com.conexa.challenge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.model.Person;
import com.conexa.challenge.domain.port.out.StarWarsCatalog;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StarWarsQueryServiceTest {

    private static final List<Person> PEOPLE = List.of(
            person(1, "Luke Skywalker"),
            person(4, "Darth Vader"),
            person(11, "Anakin Skywalker"),
            person(43, "Shmi Skywalker"));

    private final FakeCatalog catalog = new FakeCatalog();
    private final StarWarsQueryService<Person> service = new StarWarsQueryService<>(catalog);

    @ParameterizedTest(name = "id={0}, name={1} -> {2}")
    @CsvSource(nullValues = "null", value = {
            "1,    null,  Luke Skywalker",
            "1,    sky,   Luke Skywalker",
            "1,    SKY,   Luke Skywalker",
            "1,    vader, ''",
            "9999, null,  ''",
            "9999, sky,   ''"
    })
    void filtersByIdAndOptionalName(int id, String name, String expectedName) {
        PageResult<Person> result = service.find(id, name, 1, 10);

        List<String> expected = expectedName.isEmpty() ? List.of() : List.of(expectedName);
        assertThat(result.content()).extracting(Person::name).isEqualTo(expected);
        assertThat(result.totalElements()).isEqualTo(expected.size());
    }

    @Test
    void idFilterOnSecondPageIsEmptyButReportsTheMatch() {
        PageResult<Person> result = service.find(1, null, 2, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void nameFilterPaginatesMatchesInMemory() {
        PageResult<Person> result = service.find(null, "sky", 2, 2);

        assertThat(result.content()).extracting(Person::name).containsExactly("Shmi Skywalker");
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void nameWithoutMatchesReturnsEmptyPage() {
        assertThat(service.find(null, "zzz", 1, 10).content()).isEmpty();
    }

    @Test
    void withoutFiltersDelegatesPaginationToTheCatalog() {
        PageResult<Person> result = service.find(null, null, 1, 2);

        assertThat(catalog.requestedPage).isEqualTo(1);
        assertThat(catalog.requestedSize).isEqualTo(2);
        assertThat(result.content()).extracting(Person::name).containsExactly("Luke Skywalker", "Darth Vader");
    }

    private static Person person(int id, String name) {
        return new Person(id, name, null, null, null, null, null, null, null);
    }

    private static final class FakeCatalog implements StarWarsCatalog<Person> {

        private Integer requestedPage;
        private Integer requestedSize;

        @Override
        public PageResult<Person> findPage(int page, int size) {
            requestedPage = page;
            requestedSize = size;
            return PageResult.of(PEOPLE, page, size);
        }

        @Override
        public Optional<Person> findById(int id) {
            return PEOPLE.stream().filter(person -> person.id() == id).findFirst();
        }

        @Override
        public List<Person> findByName(String name) {
            return PEOPLE.stream().filter(person -> person.nameContains(name)).toList();
        }
    }
}
