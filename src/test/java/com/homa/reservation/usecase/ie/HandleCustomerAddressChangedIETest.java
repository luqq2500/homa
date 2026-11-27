package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.HOME_ARRIVAL_NOTE;
import static com.homa.reservation.usecase.ReservationTestBed.HOME_STREET_ADDRESS;
import static com.homa.reservation.usecase.ReservationTestBed.NEW_ARRIVAL_NOTE;
import static com.homa.reservation.usecase.ReservationTestBed.NEW_STREET_ADDRESS;
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
                true, false, new ServiceAddress(NEW_STREET_ADDRESS, CYBERJAYA, NEW_ARRIVAL_NOTE)));
        assertThat(bed.reload(booked).booking().address()).isEqualTo(ReservationTestBed.homeIn(AISYAH_CITY));
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Aisyah changes only her arrival note: her next booking reads the new note, Saturday's keeps the old.")
    void newArrivalNoteAppliesOnlyToNewBookings() {
        Reservation booked = bed.requestStandard();

        bed.customerAddressChanged.handle(new CustomerAddressChangedIE(bed.nextInboundId(), AISYAH_CUSTOMER_ID,
                AISYAH_CITY, HOME_STREET_ADDRESS, "Guardhouse: say unit B-12-3, then lift B", "ACTIVE",
                bed.time.now()));

        assertThat(bed.customers.findById(AISYAH_CUSTOMER_ID).orElseThrow().homeAddress()).isEqualTo(
                new ServiceAddress(HOME_STREET_ADDRESS, AISYAH_CITY, "Guardhouse: say unit B-12-3, then lift B"));
        assertThat(bed.reload(booked).booking().address().arrivalNote()).isEqualTo(HOME_ARRIVAL_NOTE);
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
