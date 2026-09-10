package com.innovatiopr.payments.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    private static final DomainError ERROR = new MoneyError.InvalidAmount("must be positive");

    @Test
    void a_success_carries_its_value_and_no_errors() {
        Result<String> result = Result.success("ok");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.orElseThrow()).isEqualTo("ok");
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void a_failure_carries_its_errors_and_refuses_to_be_unwrapped() {
        Result<String> result = Result.failure(ERROR);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.firstError().code()).isEqualTo("MONEY_INVALID_AMOUNT");
        assertThatThrownBy(result::orElseThrow).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void a_failure_must_carry_at_least_one_error() {
        assertThatThrownBy(() -> Result.failure(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void map_transforms_a_success_and_leaves_a_failure_alone() {
        assertThat(Result.success(2).map(value -> value * 3).orElseThrow()).isEqualTo(6);
        Result<Integer> failure = Result.failure(ERROR);
        assertThat(failure.map(value -> value * 3).errors()).containsExactly(ERROR);
    }

    @Test
    void flatMap_chains_operations_and_short_circuits_on_the_first_failure() {
        Result<Integer> chained = Result.success(2)
                .flatMap(value -> Result.success(value + 1))
                .flatMap(value -> Result.<Integer>failure(ERROR))
                .flatMap(value -> Result.success(value * 100));
        assertThat(chained.isFailure()).isTrue();
        assertThat(chained.errors()).containsExactly(ERROR);
    }

    @Test
    void fold_collapses_both_branches_to_one_type() {
        String fromSuccess = Result.success(5).fold(value -> "value " + value, errors -> "failed");
        String fromFailure = Result.<Integer>failure(ERROR).fold(value -> "value " + value, errors -> "failed");
        assertThat(fromSuccess).isEqualTo("value 5");
        assertThat(fromFailure).isEqualTo("failed");
    }

    @Test
    void propagate_retypes_a_failure_without_losing_its_errors() {
        Result<String> source = Result.failure(ERROR);
        Result<Integer> propagated = source.propagate();
        assertThat(propagated.errors()).containsExactly(ERROR);
    }

    @Test
    void propagate_refuses_to_retype_a_success() {
        assertThatThrownBy(() -> Result.success("ok").propagate()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pattern_matching_over_the_sealed_type_is_exhaustive_without_a_default() {
        Result<String> result = Result.success("hello");
        String described = switch (result) {
            case Result.Success<String>(String value) -> "ok:" + value;
            case Result.Failure<String> failure -> "err:" + failure.errors().size();
        };
        assertThat(described).isEqualTo("ok:hello");
    }

    @Test
    void ok_produces_a_payload_free_success() {
        assertThat(Result.ok().isSuccess()).isTrue();
    }
}
