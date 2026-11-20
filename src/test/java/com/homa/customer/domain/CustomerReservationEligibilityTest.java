package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.aisyah;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerReservationEligibilityTest {

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah's husband browses Kemas Home without an account. When he taps Reserve, he's asked to "
            + "register first.")
    void onlyARegisteredCustomerCanMakeReservations() {
        assertThat(aisyah().canMakeReservations()).isTrue();

        assertThat(Customer.class.getConstructors()).isEmpty();
        assertThat(Arrays.stream(Customer.class.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == Customer.class)
                .map(method -> method.getName()))
                .containsExactly("register");
    }
}
