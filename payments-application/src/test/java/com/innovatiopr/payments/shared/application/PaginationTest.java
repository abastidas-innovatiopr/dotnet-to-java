package com.innovatiopr.payments.shared.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {

    private static final SortSpec SORT = SortSpec.descending("createdAt");

    @ParameterizedTest(name = "requested size {0} becomes {1}")
    @CsvSource({
            "20, 20",
            "1, 1",
            "100, 100",
            "101, 100",     // clamped to MAX_SIZE: no client can request an unbounded page
            "1000, 100",
            "0, 20",        // meaningless, so the default applies
            "-5, 20"
    })
    void normalises_page_size(int requested, int expected) {
        assertThat(PageRequest.of(0, requested, SORT).size()).isEqualTo(expected);
    }

    @Test
    void normalises_a_negative_page_to_the_first_page() {
        assertThat(PageRequest.of(-3, 20, SORT).page()).isZero();
    }

    @Test
    void computes_the_offset_as_a_long_so_deep_paging_cannot_overflow() {
        assertThat(PageRequest.of(0, 20, SORT).offset()).isZero();
        assertThat(PageRequest.of(3, 20, SORT).offset()).isEqualTo(60L);
        assertThat(PageRequest.of(200_000_000, 100, SORT).offset()).isEqualTo(20_000_000_000L);
    }

    @Test
    void first_page_has_a_next_but_no_previous() {
        PageResult<String> page = new PageResult<>(List.of("a", "b"), 0, 2, 5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void middle_page_has_both() {
        PageResult<String> page = new PageResult<>(List.of("c", "d"), 1, 2, 5);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void last_page_has_a_previous_but_no_next() {
        PageResult<String> page = new PageResult<>(List.of("e"), 2, 2, 5);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void an_empty_result_has_no_pages_and_no_navigation() {
        PageResult<String> page = new PageResult<>(List.of(), 0, 20, 0);
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    void total_pages_rounds_up_for_a_partial_final_page() {
        assertThat(new PageResult<>(List.of(), 0, 20, 21).totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(), 0, 20, 40).totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(), 0, 20, 41).totalPages()).isEqualTo(3);
    }

    @Test
    void items_are_defensively_copied() {
        List<String> mutable = new java.util.ArrayList<>(List.of("a"));
        PageResult<String> page = new PageResult<>(mutable, 0, 20, 1);
        mutable.add("b");
        assertThat(page.items()).containsExactly("a");
    }
}
