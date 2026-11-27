package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static com.homa.reservation.usecase.ReservationTestBed.UNVERIFIED_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.standardCheckout;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleCustomerVerifiedIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private CustomerVerifiedIE verified(String eventId, String customerId) {
        return new CustomerVerifiedIE(eventId, customerId, "person-husband", TAPPED_PAY_AT);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah's husband verifies his identity, and can then start paying for a reservation.")
    void verifiedCustomerCanReserve() {
        bed.customerVerified.handle(verified("verified-1", UNVERIFIED_CUSTOMER_ID));

        assertThat(bed.customers.findById(UNVERIFIED_CUSTOMER_ID)).contains(new CustomerStanding(
                UNVERIFIED_CUSTOMER_ID, true, false, new ServiceAddress(AISYAH_CITY, AISYAH_CITY, "")));
        assertThat(bed.startCheckout.start(standardCheckout(UNVERIFIED_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID))
                .outcome()).isEqualTo("STARTED");
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("A verification for a customer Reservation never heard of adds no one.")
    void verificationOfAnUnknownCustomerAddsNoOne() {
        int customers = bed.customers.count();

        bed.customerVerified.handle(verified("verified-1", "customer-unknown"));

        assertThat(bed.customers.findById("customer-unknown")).isEmpty();
        assertThat(bed.customers.count()).isEqualTo(customers);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("A redelivered verification changes nothing.")
    void redeliveredVerificationIsIgnored() {
        CustomerVerifiedIE event = verified("verified-1", UNVERIFIED_CUSTOMER_ID);
        bed.customerVerified.handle(event);
        CustomerStanding suspended = new CustomerStanding(UNVERIFIED_CUSTOMER_ID, false, true,
                new ServiceAddress(AISYAH_CITY, AISYAH_CITY, ""));
        bed.customers.save(suspended);

        bed.customerVerified.handle(event);

        assertThat(bed.customers.findById(UNVERIFIED_CUSTOMER_ID)).contains(suspended);
        assertThat(HandleCustomerVerifiedIE.HANDLER).isEqualTo("reservation.CustomerVerified");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
