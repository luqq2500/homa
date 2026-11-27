package com.homa.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaymentIdsTest {

    static Stream<Arguments> everyId() {
        return Stream.of(
                Arguments.of("PaymentHoldId", (Function<String, Object>) PaymentHoldId::new),
                Arguments.of("PayoutId", (Function<String, Object>) PayoutId::new),
                Arguments.of("PaymentAttemptId", (Function<String, Object>) PaymentAttemptId::new),
                Arguments.of("CheckoutId", (Function<String, Object>) CheckoutId::new),
                Arguments.of("CustomerId", (Function<String, Object>) CustomerId::new),
                Arguments.of("ProviderId", (Function<String, Object>) ProviderId::new),
                Arguments.of("ReservationId", (Function<String, Object>) ReservationId::new),
                Arguments.of("ReportId", (Function<String, Object>) ReportId::new),
                Arguments.of("PayoutAccountId", (Function<String, Object>) PayoutAccountId::new));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyId")
    void idIsNeverMissingOrBlank(String idName, Function<String, Object> newId) {
        assertThatThrownBy(() -> newId.apply(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(idName);
        assertThatThrownBy(() -> newId.apply(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newId.apply("")).isInstanceOf(IllegalArgumentException.class);
        assertThat(newId.apply("x")).isEqualTo(newId.apply("x"));
    }

    @Test
    void generatedPaymentHoldIdsAreUnique() {
        PaymentHoldId first = PaymentHoldId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(PaymentHoldId.generate());
    }

    @Test
    void generatedPaymentAttemptIdsAreUnique() {
        PaymentAttemptId first = PaymentAttemptId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(PaymentAttemptId.generate());
    }

    @Test
    void generatedPayoutAccountIdsAreUnique() {
        PayoutAccountId first = PayoutAccountId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(PayoutAccountId.generate());
    }

    @Test
    void generatedPayoutIdsAreUnique() {
        PayoutId first = PayoutId.generate();

        assertThat(first.value()).isNotBlank();
        assertThat(first).isNotEqualTo(PayoutId.generate());
    }
}
