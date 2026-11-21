package com.homa.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.reservation.domain.exception.BookingRefusedException;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class BookingRefusalTest {

    @Test
    @Tag("RES-11")
    @DisplayName("Mei is told the slot was taken only when another customer's payment took Kemas Home's time first; any other refusal is a booking rule that no longer passes.")
    void onlyATakenProviderTimeIsRejectedAsTimeTaken() {
        assertThat(BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION.rejection()).isEqualTo(CheckoutRejection.TIME_TAKEN);
        assertThat(Arrays.stream(BookingRefusal.values())
                .filter(refusal -> refusal != BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION))
                .allSatisfy(refusal -> assertThat(refusal.rejection()).isEqualTo(CheckoutRejection.RULE_FAILED));
        assertThat(new BookingRefusedException(BookingRefusal.CUSTOMER_BUSY).rejection())
                .isEqualTo(CheckoutRejection.RULE_FAILED);
    }
}
