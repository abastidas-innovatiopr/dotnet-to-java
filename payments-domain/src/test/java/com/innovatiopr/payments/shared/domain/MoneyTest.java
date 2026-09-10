package com.innovatiopr.payments.shared.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Money is a pure value object, so these tests need no Spring context, no database and no mocks.
 * They run in milliseconds, which is what makes it reasonable to have a lot of them.
 */
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Nested
    @DisplayName("scale and rounding")
    class ScaleAndRounding {

        @Test
        void normalises_to_the_currency_minor_unit() {
            assertThat(Money.of("10", USD).amount()).isEqualTo(new BigDecimal("10.00"));
            assertThat(Money.of("10.5", USD).amount()).isEqualTo(new BigDecimal("10.50"));
        }

        @Test
        void japanese_yen_has_no_minor_unit() {
            assertThat(Money.of("1000", JPY).amount().scale()).isZero();
            assertThat(Money.of("1000.4", JPY).amount()).isEqualTo(new BigDecimal("1000"));
        }

        @ParameterizedTest(name = "{0} -> {1} (banker''s rounding)")
        @CsvSource({
                "2.005, 2.00",   // ties go to the even neighbour, not away from zero
                "2.015, 2.02",
                "2.025, 2.02",
                "2.035, 2.04"
        })
        void rounds_half_to_even(String input, String expected) {
            // HALF_UP would give 2.01, 2.02, 2.03, 2.04 - biased upward across many roundings.
            assertThat(Money.of(input, USD).amount()).isEqualTo(new BigDecimal(expected));
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        void amounts_that_differ_only_in_scale_are_equal() {
            // BigDecimal alone would say these differ: new BigDecimal("10.0").equals("10.00") is false.
            // Normalising on construction is what makes Money behave like a value object.
            assertThat(Money.of("10", USD)).isEqualTo(Money.of("10.00", USD));
            assertThat(Money.of("10", USD)).hasSameHashCodeAs(Money.of("10.000", USD));
        }

        @Test
        void the_same_amount_in_different_currencies_is_not_equal() {
            assertThat(Money.of("10.00", USD)).isNotEqualTo(Money.of("10.00", EUR));
        }
    }

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        void adds_and_subtracts() {
            assertThat(Money.of("10.00", USD).add(Money.of("5.50", USD))).isEqualTo(Money.of("15.50", USD));
            assertThat(Money.of("10.00", USD).subtract(Money.of("2.25", USD))).isEqualTo(Money.of("7.75", USD));
        }

        @Test
        void is_immutable() {
            Money original = Money.of("10.00", USD);
            original.add(Money.of("90.00", USD));
            assertThat(original).isEqualTo(Money.of("10.00", USD));
        }

        @Test
        void subtraction_may_produce_a_negative_value() {
            // Money itself allows negatives; it is Account that forbids a negative balance.
            assertThat(Money.of("5.00", USD).subtract(Money.of("8.00", USD)).isNegative()).isTrue();
        }

        @Test
        void refuses_to_add_different_currencies() {
            assertThatThrownBy(() -> Money.of("10.00", USD).add(Money.of("10.00", EUR)))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessageContaining("USD")
                    .hasMessageContaining("EUR");
        }

        @Test
        void refuses_to_compare_different_currencies() {
            assertThatThrownBy(() -> Money.of("10.00", USD).greaterThan(Money.of("1.00", EUR)))
                    .isInstanceOf(CurrencyMismatchException.class);
        }
    }

    @Nested
    @DisplayName("predicates")
    class Predicates {

        @Test
        void reports_sign() {
            assertThat(Money.of("1.00", USD).isPositive()).isTrue();
            assertThat(Money.zero(USD).isZero()).isTrue();
            assertThat(Money.zero(USD).isPositive()).isFalse();
            assertThat(Money.of("-1.00", USD).isNegative()).isTrue();
        }

        @Test
        void compares_within_a_currency() {
            Money ten = Money.of("10.00", USD);
            Money twenty = Money.of("20.00", USD);
            assertThat(ten.lessThan(twenty)).isTrue();
            assertThat(twenty.greaterThan(ten)).isTrue();
            assertThat(ten.greaterThanOrEqualTo(Money.of("10.000", USD))).isTrue();
            assertThat(ten.sameCurrency(twenty)).isTrue();
        }
    }

    @Test
    void rejects_an_unknown_currency_code() {
        assertThatThrownBy(() -> Money.of("1.00", "XYZ")).isInstanceOf(IllegalArgumentException.class);
    }
}
