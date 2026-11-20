package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.daniel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.CustomerNotSuspendedException;
import com.homa.customer.domain.exception.MissingSuspensionReasonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerSuspensionTest {

    private static final String SAFETY_AND_MISCONDUCT =
            "Confirmed Safety & Misconduct report: shouted at and threatened Kemas Home's helpers";

    @Test
    @Tag("CUS-4")
    @DisplayName("Kemas Home reports that a customer, Daniel, shouted at and threatened its helpers. Support confirms "
            + "the report and suspends Daniel.")
    void supportSuspendsACustomerAfterAConfirmedSafetyAndMisconductReport() {
        Customer daniel = daniel();

        daniel.suspend(SAFETY_AND_MISCONDUCT);

        assertThat(daniel.status()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(daniel.suspensionReason()).contains(SAFETY_AND_MISCONDUCT);
        assertThat(daniel.canMakeReservations()).isFalse();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Support must say why it suspends Daniel, and can't suspend him twice.")
    void supportSuspendsACustomerOnlyWithAReasonAndOnlyOnce() {
        Customer daniel = daniel();

        assertThatThrownBy(() -> daniel.suspend(null)).isInstanceOf(MissingSuspensionReasonException.class);
        assertThatThrownBy(() -> daniel.suspend(" ")).isInstanceOf(MissingSuspensionReasonException.class)
                .hasMessageContaining("CUS-4");
        assertThat(daniel.status()).isEqualTo(CustomerStatus.ACTIVE);

        daniel.suspend(SAFETY_AND_MISCONDUCT);

        assertThatThrownBy(() -> daniel.suspend("Repeated confirmed no-shows"))
                .isInstanceOf(CustomerAlreadySuspendedException.class)
                .hasMessageContaining(daniel.id().value());
        assertThat(daniel.suspensionReason()).contains(SAFETY_AND_MISCONDUCT);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A month later Support reviews Daniel's appeal and reactivates his account.")
    void supportReactivatesASuspendedCustomerAfterReviewingTheAppeal() {
        Customer daniel = daniel();
        daniel.suspend(SAFETY_AND_MISCONDUCT);

        daniel.reactivate();

        assertThat(daniel.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(daniel.suspensionReason()).isEmpty();
        assertThat(daniel.canMakeReservations()).isTrue();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel's account is active, so there is nothing for Support to reactivate.")
    void activeCustomerCannotBeReactivated() {
        Customer daniel = daniel();

        assertThatThrownBy(daniel::reactivate).isInstanceOf(CustomerNotSuspendedException.class)
                .hasMessageContaining("CUS-6")
                .hasMessageContaining(daniel.id().value());
        assertThat(daniel.status()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
