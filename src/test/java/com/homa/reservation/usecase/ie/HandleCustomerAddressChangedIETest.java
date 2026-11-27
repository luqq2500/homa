package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.CustomerAddressChangedIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleCustomerAddressChangedIETest {

    private static final String CYBERJAYA = "CYBERJAYA";

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("RES-8")
    @DisplayName("Her new address on Thursday applies only to new bookings: Saturday's reservation keeps Salak Tinggi.")
    void newAddressAppliesOnlyToNewBookings() {
        Reservation booked = bed.requestStandard();

        bed.customerAddressChanged.handle(bed.moved(AISYAH_CUSTOMER_ID, CYBERJAYA));

        assertThat(bed.customers.findById(AISYAH_CUSTOMER_ID)).contains(new CustomerStanding(AISYAH_CUSTOMER_ID,
                true, false, new ServiceAddress(CYBERJAYA, CYBERJAYA, "")));
        assertThat(bed.reload(booked).booking().address().city()).isEqualTo(AISYAH_CITY);
    }

    @Test
    @Tag("RES-1")
    @DisplayName("A move by a customer Reservation never heard of adds no one.")
    void moveOfAnUnknownCustomerAddsNoOne() {
        int customers = bed.customers.count();

        bed.customerAddressChanged.handle(bed.moved("customer-unknown", CYBERJAYA));

        assertThat(bed.customers.findById("customer-unknown")).isEmpty();
        assertThat(bed.customers.count()).isEqualTo(customers);
    }

    @Test
    @Tag("RES-1")
    @DisplayName("A redelivered move changes nothing, even after a later one.")
    void redeliveredMoveIsIgnored() {
        CustomerAddressChangedIE toCyberjaya = bed.moved(AISYAH_CUSTOMER_ID, CYBERJAYA);
        bed.customerAddressChanged.handle(toCyberjaya);
        bed.customerAddressChanged.handle(bed.moved(AISYAH_CUSTOMER_ID, "PUTRAJAYA"));

        bed.customerAddressChanged.handle(toCyberjaya);

        assertThat(bed.customers.findById(AISYAH_CUSTOMER_ID).orElseThrow().homeAddress().city())
                .isEqualTo("PUTRAJAYA");
        assertThat(HandleCustomerAddressChangedIE.HANDLER).isEqualTo("reservation.CustomerAddressChanged");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
