package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.jobs;
import static reservation.domain.ReservationBuilders.nextSaturday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.EnquiryAlreadyRequestedException;
import reservation.domain.exception.EnquiryDateInPastException;
import reservation.domain.exception.ReservationDetailRequiredException;

/** Feature: Enquiries. */
class EnquiryLifecycleTest {

    private Enquiry anEnquiryForSaturday10() {
        return Enquiry.create("customer-1", "provider-1", aBooking().startingAt(saturday("10:00")).build(), thursday("09:00"));
    }

    @Test
    void anEnquiryClosesWhenItsDatePassesWithoutARequest() {
        Enquiry enquiry = anEnquiryForSaturday10();

        assertThat(enquiry.isClosed(saturday("10:00"))).isTrue();
        assertThat(enquiry.isClosed(saturday("09:59"))).isFalse();
    }

    @Test
    void aRequestedEnquiryDoesNotClose() {
        Enquiry enquiry = anEnquiryForSaturday10();
        enquiry.markRequested("reservation-1");

        assertThat(enquiry.isClosed(saturday("10:00"))).isFalse();
        assertThat(enquiry.reservationId()).contains("reservation-1");
    }

    @Test
    void anEnquiryTurnsIntoOneReservationOnly() {
        Enquiry enquiry = anEnquiryForSaturday10();
        enquiry.markRequested("reservation-1");

        assertThatThrownBy(() -> enquiry.markRequested("reservation-2"))
                .isInstanceOf(EnquiryAlreadyRequestedException.class)
                .hasMessageContaining("reservation-1");
    }

    @Test
    void bookAgainCopiesTheDetailsWithANewDate() {
        Enquiry past = Enquiry.create(
                "customer-1", "provider-1", aBooking().forJobs("KITCHEN").forHours(3).withHelpers(2).build(), thursday("09:00"));

        Enquiry again = Enquiry.bookAgainFrom(past, nextSaturday("10:00"), saturday("15:00"));

        assertThat(again.id()).isNotEqualTo(past.id());
        assertThat(again.customerId()).isEqualTo("customer-1");
        assertThat(again.providerId()).isEqualTo("provider-1");
        assertThat(again.details().jobs()).isEqualTo(jobs("KITCHEN"));
        assertThat(again.details().hours()).isEqualTo(3);
        assertThat(again.details().helpers()).isEqualTo(2);
        assertThat(again.details().start()).isEqualTo(nextSaturday("10:00"));
        assertThat(again.details().address()).isEqualTo(past.details().address());
        assertThat(again.createdAt()).isEqualTo(saturday("15:00"));
        assertThat(again.reservationId()).isEmpty();
    }

    @Test
    void anEnquiryIsForATimeStillToCome() {
        assertThatThrownBy(() -> Enquiry.create("customer-1", "provider-1", aBooking().startingAt(thursday("09:00")).build(), thursday("09:00")))
                .isInstanceOf(EnquiryDateInPastException.class);
    }

    @Test
    void anEnquiryIsBetweenACustomerAndAProvider() {
        assertThatThrownBy(() -> Enquiry.create(null, "provider-1", aBooking().build(), thursday("09:00")))
                .isInstanceOf(ReservationDetailRequiredException.class);
        assertThatThrownBy(() -> Enquiry.create("customer-1", "", aBooking().build(), thursday("09:00")))
                .isInstanceOf(ReservationDetailRequiredException.class);
        assertThatThrownBy(() -> anEnquiryForSaturday10().markRequested(" "))
                .isInstanceOf(ReservationDetailRequiredException.class);
    }
}
