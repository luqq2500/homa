package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.AISYAH_AGREED_AT;
import static com.homa.customer.domain.CustomerFixtures.AISYAH_ARRIVAL_NOTE;
import static com.homa.customer.domain.CustomerFixtures.AISYAH_STREET_ADDRESS;
import static com.homa.customer.domain.CustomerFixtures.aisyahJustRegistered;
import static com.homa.customer.domain.CustomerFixtures.aisyahsArrivalNote;
import static com.homa.customer.domain.CustomerFixtures.aisyahsGoogleAccount;
import static com.homa.customer.domain.CustomerFixtures.aisyahsHome;
import static com.homa.customer.domain.CustomerFixtures.aisyahsName;
import static com.homa.customer.domain.CustomerFixtures.aisyahsTermsAcceptance;
import static com.homa.customer.domain.CustomerFixtures.salakTinggi;
import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.MissingCustomerDetailException;
import com.homa.customer.domain.exception.TermsOfServiceNotAcceptedException;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerRegistrationTest {

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah enters her name, her Salak Tinggi address and \"Guardhouse: say unit B-12-3\". No phone "
            + "number is asked for.")
    void customerRegistersWithANameAHomeAddressWithCityAndAnArrivalNote() {
        Customer aisyah = aisyahJustRegistered();

        assertThat(aisyah.name().value()).isEqualTo(AISYAH_NAME);
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo(AISYAH_STREET_ADDRESS);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo(AISYAH_CITY);
        assertThat(aisyah.arrivalNote().value()).isEqualTo(AISYAH_ARRIVAL_NOTE);
        assertThat(aisyah.termsAcceptance().acceptedAt()).isEqualTo(AISYAH_AGREED_AT);
        assertThat(aisyah.signInId()).isEqualTo(aisyahsGoogleAccount());
        assertThat(aisyah.statusAt(AISYAH_AGREED_AT)).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(aisyah.suspension()).isEmpty();
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Each registration gets its own customer id.")
    void customerGetsANewIdOnRegistration() {
        Customer aisyah = aisyahJustRegistered();

        assertThat(aisyah.id()).isNotNull().isNotEqualTo(aisyahJustRegistered().id());
        assertThat(UUID.fromString(aisyah.id().value())).isNotNull();
        assertThat(CustomerId.generate()).isNotEqualTo(CustomerId.generate());
        assertThat(new CustomerId("customer-aisyah").value()).isEqualTo("customer-aisyah");
        assertThatThrownBy(() -> new CustomerId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CustomerId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah can't finish registering while her name, her home address or her arrival note is missing.")
    void customerCannotRegisterWithADetailMissing() {
        assertMissing(() -> Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), null, aisyahsHome(),
                aisyahsArrivalNote(), aisyahsTermsAcceptance()), "a name");
        assertMissing(() -> Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), aisyahsName(), null,
                aisyahsArrivalNote(), aisyahsTermsAcceptance()), "a home address");
        assertMissing(() -> Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), aisyahsName(),
                aisyahsHome(), null, aisyahsTermsAcceptance()), "an arrival note");
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("A blank name, street address, city or arrival note counts as missing.")
    void customerDetailIsMissingWhenBlank() {
        assertMissing(() -> new CustomerName(""), "a name");
        assertMissing(() -> new CustomerName(null), "a name");
        assertMissing(() -> new HomeAddress("\t", salakTinggi()), "a home address");
        assertMissing(() -> new HomeAddress(null, salakTinggi()), "a home address");
        assertMissing(() -> new HomeAddress(AISYAH_STREET_ADDRESS, null), "a city in the home address");
        assertMissing(() -> new CityCode(" "), "a city in the home address");
        assertMissing(() -> new CityCode(null), "a city in the home address");
        assertMissing(() -> new ArrivalNote("  "), "an arrival note");
        assertMissing(() -> new ArrivalNote(null), "an arrival note");
    }

    @Test
    @Tag("CUS-2")
    @DisplayName("Aisyah can't finish registering until she ticks the Terms of Service.")
    void customerCannotRegisterWithoutAgreeingToTheTermsOfService() {
        assertThatThrownBy(() -> TermsAcceptance.of(false, AISYAH_AGREED_AT))
                .isInstanceOf(TermsOfServiceNotAcceptedException.class)
                .hasMessageContaining("CUS-2");
        assertThatThrownBy(() -> Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), aisyahsName(),
                aisyahsHome(), aisyahsArrivalNote(), null))
                .isInstanceOf(TermsOfServiceNotAcceptedException.class);
        assertThatThrownBy(() -> new TermsAcceptance(null)).isInstanceOf(TermsOfServiceNotAcceptedException.class);

        TermsAcceptance ticked = TermsAcceptance.of(true, AISYAH_AGREED_AT);
        assertThat(ticked.acceptedAt()).isEqualTo(AISYAH_AGREED_AT);
        assertThat(Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), aisyahsName(), aisyahsHome(),
                aisyahsArrivalNote(), ticked).termsAcceptance()).isEqualTo(ticked);
    }

    static void assertMissing(ThrowingCallable registration, String detail) {
        assertThatThrownBy(registration)
                .isInstanceOfSatisfying(MissingCustomerDetailException.class,
                        e -> assertThat(e.detail()).isEqualTo(detail))
                .hasMessage("CUS-1: a customer must have " + detail);
    }
}
