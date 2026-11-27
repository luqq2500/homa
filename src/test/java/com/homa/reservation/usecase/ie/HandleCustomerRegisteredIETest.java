package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleCustomerRegisteredIETest {

    private final ReservationTestBed bed = new ReservationTestBed();

    private CustomerRegisteredIE registered(String eventId, String city) {
        return new CustomerRegisteredIE(eventId, "customer-new", city, "ACTIVE", TAPPED_PAY_AT);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("A newly registered customer in Salak Tinggi isn't identity verified yet, so can't reserve until they verify.")
    void registeredCustomerIsNotYetVerified() {
        bed.customerRegistered.handle(registered("registered-1", AISYAH_CITY));

        CustomerStanding standing = bed.customers.findById("customer-new").orElseThrow();
        assertThat(standing).isEqualTo(CustomerStanding.registered("customer-new",
                new ServiceAddress(AISYAH_CITY, AISYAH_CITY, "")));
        assertThat(standing.refusal()).contains(BookingRefusal.CUSTOMER_NOT_VERIFIED);
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("A redelivered registration changes nothing.")
    void redeliveredRegistrationIsIgnored() {
        CustomerRegisteredIE event = registered("registered-1", AISYAH_CITY);
        bed.customerRegistered.handle(event);
        bed.customers.save(bed.customers.findById("customer-new").orElseThrow().verified());

        bed.customerRegistered.handle(event);

        assertThat(bed.customers.findById("customer-new").orElseThrow().identityVerified()).isTrue();
        assertThat(bed.customers.count()).isEqualTo(1);
        assertThat(HandleCustomerRegisteredIE.HANDLER).isEqualTo("reservation.CustomerRegistered");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
