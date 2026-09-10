package com.innovatiopr.payments.shared.infrastructure;

import com.innovatiopr.payments.shared.application.SortDirection;
import com.innovatiopr.payments.shared.application.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SortColumnsTest {

    private final SortColumns sortable = new SortColumns(
            Map.of("createdAt", "created_at", "amount", "amount", "status", "status"),
            "created_at",
            "id");

    @Test
    void maps_an_allowed_field_to_its_column_and_appends_the_tiebreaker() {
        assertThat(sortable.orderByClause(SortSpec.of("amount", SortDirection.ASC)))
                .isEqualTo("amount ASC, id ASC");
    }

    @Test
    void falls_back_to_the_default_column_for_an_unknown_field() {
        assertThat(sortable.orderByClause(SortSpec.of("nonsense", SortDirection.DESC)))
                .isEqualTo("created_at DESC, id DESC");
    }

    @Test
    void never_lets_client_input_reach_the_sql() {
        // ORDER BY cannot be a bind parameter, so the column name is part of the statement text.
        // The allow-list is what guarantees an injection attempt is discarded rather than escaped.
        String clause = sortable.orderByClause(
                SortSpec.of("amount; DROP TABLE accounts--", SortDirection.DESC));

        assertThat(clause).isEqualTo("created_at DESC, id DESC");
        assertThat(clause).doesNotContain("DROP");
    }

    @Test
    void does_not_duplicate_the_tiebreaker_when_it_is_already_the_sort_column() {
        SortColumns byId = new SortColumns(Map.of("id", "id"), "id", "id");
        assertThat(byId.orderByClause(SortSpec.of("id", SortDirection.ASC))).isEqualTo("id ASC");
    }

    @Test
    void exposes_the_fields_it_supports() {
        assertThat(sortable.supportedFields()).containsExactlyInAnyOrder("createdAt", "amount", "status");
        assertThat(sortable.supports("amount")).isTrue();
        assertThat(sortable.supports("secret_column")).isFalse();
    }
}
