package com.homa.customer.usecase;

import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_ARRIVAL_NOTE;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_STREET_ADDRESS;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.CYBERJAYA;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.CYBERJAYA_ARRIVAL_NOTE;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.CYBERJAYA_STREET_ADDRESS;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.DANIEL_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahJustRegistered;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahMovesToCyberjaya;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.registeredWithGoogle;
import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.Suspension;
import com.homa.customer.domain.SuspensionLength;
import com.homa.customer.domain.exception.MissingCustomerDetailException;
import com.homa.customer.usecase.dto.UpdateProfileRequest;
import com.homa.customer.usecase.dto.UpdateProfileResponse;
import com.homa.customer.usecase.exception.CustomerNotFoundException;
import com.homa.customer.usecase.exception.NotAllowedException;
import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.CustomerAddressChangedIE;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerUpdateProfileTest {

    /** Aisyah moves to Cyberjaya and saves her profile on Tuesday 1 December at 09:00. */
    private static final Instant SAVED_AT = HomaFixtures.at(2026, 12, 1, 9, 0);

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final RecordingCustomerIEPublisher publisher = new RecordingCustomerIEPublisher();
    private final CustomerUpdateProfile updateProfile = new CustomerUpdateProfile(customers, publisher,
            new FixedTimeSource(SAVED_AT), new SequentialEventIds());

    private final Customer aisyah = customers.given(aisyahJustRegistered());
    private final String aisyahsId = aisyah.id().value();

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah moves from Salak Tinggi to Cyberjaya. She enters her new address, with its city, and a new "
            + "arrival note, and Reservation hears of her new city.")
    void customerMovesHomeAndReservationHearsOfHerNewCity() {
        UpdateProfileResponse response = updateProfile.update(aisyahMovesToCyberjaya(aisyahsId));

        assertThat(customers.saves()).containsExactly(aisyah.id());
        assertThat(aisyah.name().value()).isEqualTo("Aisyah R.");
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo(CYBERJAYA_STREET_ADDRESS);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo(CYBERJAYA);
        assertThat(aisyah.arrivalNote().value()).isEqualTo(CYBERJAYA_ARRIVAL_NOTE);
        assertThat(response).isEqualTo(new UpdateProfileResponse(aisyahsId, "Aisyah R.", CYBERJAYA_STREET_ADDRESS,
                CYBERJAYA, CYBERJAYA_ARRIVAL_NOTE));

        assertThat(publisher.published()).hasSize(1);
        CustomerAddressChangedIE moved = (CustomerAddressChangedIE) publisher.published().getFirst();
        assertThat(moved.eventId()).isEqualTo("event-1");
        assertThat(moved.customerId()).isEqualTo(aisyahsId);
        assertThat(moved.city()).isEqualTo(CYBERJAYA);
        assertThat(moved.status()).isEqualTo("ACTIVE");
        assertThat(moved.occurredAt()).isEqualTo(SAVED_AT);
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah changes how her name is written and her arrival note, at the same home: nothing to tell "
            + "Reservation.")
    void customerWhoStaysHomeIsNotAnnouncedAsMoved() {
        UpdateProfileResponse response = updateProfile.update(new UpdateProfileRequest(aisyahsId, aisyahsId,
                "Aisyah R.", AISYAH_STREET_ADDRESS, AISYAH_CITY, "Guardhouse: say unit B-12-3, then lift B"));

        assertThat(customers.saves()).containsExactly(aisyah.id());
        assertThat(aisyah.name().value()).isEqualTo("Aisyah R.");
        assertThat(aisyah.arrivalNote().value()).isEqualTo("Guardhouse: say unit B-12-3, then lift B");
        assertThat(response.city()).isEqualTo(AISYAH_CITY);
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Daniel, suspended for 30 days, moves home: Reservation hears of his new city and that he is "
            + "suspended.")
    void aSuspendedCustomersMoveCarriesHisStanding() {
        Customer daniel = customers.given(registeredWithGoogle(DANIEL_GOOGLE_SUBJECT, "Daniel"));
        daniel.suspend(Suspension.decided(SuspensionLength.THIRTY_DAYS, HomaFixtures.at(2026, 12, 31, 9, 0),
                "Confirmed Safety & Misconduct report", HomaFixtures.at(2026, 12, 1, 8, 0)));
        String danielsId = daniel.id().value();

        updateProfile.update(new UpdateProfileRequest(danielsId, danielsId, "Daniel", CYBERJAYA_STREET_ADDRESS,
                CYBERJAYA, CYBERJAYA_ARRIVAL_NOTE));

        CustomerAddressChangedIE moved = (CustomerAddressChangedIE) publisher.published().getFirst();
        assertThat(moved.customerId()).isEqualTo(danielsId);
        assertThat(moved.status()).isEqualTo("SUSPENDED");
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Daniel, signed in, can't change Aisyah's profile.")
    void customerCanChangeOnlyTheirOwnProfile() {
        Customer daniel = customers.given(registeredWithGoogle(DANIEL_GOOGLE_SUBJECT, "Daniel"));
        UpdateProfileRequest danielEditsAisyah = new UpdateProfileRequest(daniel.id().value(), aisyahsId,
                "Aisyah R.", CYBERJAYA_STREET_ADDRESS, CYBERJAYA, CYBERJAYA_ARRIVAL_NOTE);

        assertThatThrownBy(() -> updateProfile.update(danielEditsAisyah)).isInstanceOf(NotAllowedException.class);

        assertProfileUnchanged();
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("No one who isn't signed in can change a profile.")
    void aProfileChangeNeedsASignedInCustomer() {
        UpdateProfileRequest anonymous = new UpdateProfileRequest(null, aisyahsId, "Aisyah R.",
                CYBERJAYA_STREET_ADDRESS, CYBERJAYA, CYBERJAYA_ARRIVAL_NOTE);

        assertThatThrownBy(() -> updateProfile.update(anonymous)).isInstanceOf(NotAllowedException.class);

        assertProfileUnchanged();
    }

    @Test
    @DisplayName("A profile of an account Homa doesn't have can't be changed.")
    void anUnknownCustomersProfileCantBeChanged() {
        assertThatThrownBy(() -> updateProfile.update(aisyahMovesToCyberjaya("customer-unknown")))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("customer-unknown");

        assertProfileUnchanged();
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah can't clear her arrival note; her account keeps her name, home and note as they were.")
    void aProfileWithADetailLeftOutChangesNothing() {
        UpdateProfileRequest noArrivalNote = new UpdateProfileRequest(aisyahsId, aisyahsId, "Aisyah R.",
                CYBERJAYA_STREET_ADDRESS, CYBERJAYA, " ");

        assertThatThrownBy(() -> updateProfile.update(noArrivalNote))
                .isInstanceOfSatisfying(MissingCustomerDetailException.class,
                        missing -> assertThat(missing.detail()).isEqualTo("an arrival note"));

        assertProfileUnchanged();
    }

    private void assertProfileUnchanged() {
        assertThat(aisyah.name().value()).isEqualTo(AISYAH_NAME);
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo(AISYAH_STREET_ADDRESS);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo(AISYAH_CITY);
        assertThat(aisyah.arrivalNote().value()).isEqualTo(AISYAH_ARRIVAL_NOTE);
        assertThat(customers.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }
}
