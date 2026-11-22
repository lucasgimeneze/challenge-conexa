package com.conexa.challenge.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageResultTest {

    private static final List<String> ITEMS = List.of("a", "b", "c", "d", "e");

    @Test
    void slicesTheRequestedPage() {
        PageResult<String> page = PageResult.of(ITEMS, 2, 2);

        assertThat(page.content()).containsExactly("c", "d");
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void lastPageMayBeIncomplete() {
        assertThat(PageResult.of(ITEMS, 3, 2).content()).containsExactly("e");
    }

    @Test
    void pageBeyondTheLastIsEmptyButKeepsTotals() {
        PageResult<String> page = PageResult.of(ITEMS, 4, 2);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @ParameterizedTest(name = "page={0}, size={1}, totalElements={2}")
    @CsvSource({"0, 10, 0", "1, 0, 0", "1, 10, -1"})
    void rejectsInvalidBounds(int page, int size, long totalElements) {
        assertThatThrownBy(() -> PageResult.empty(page, size, totalElements))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyListHasNoPages() {
        PageResult<String> page = PageResult.of(List.of(), 1, 10);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalPages()).isZero();
    }
}
