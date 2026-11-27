package com.homa.customer.usecase;

import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_ARRIVAL_NOTE;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_STREET_ADDRESS;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.APPLE;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.GOOGLE;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.REGISTERED_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahRegisters;
import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.SignInId;
import com.homa.customer.domain.exception.MissingCustomerDetailException;
import com.homa.customer.domain.exception.MissingSignInException;
import com.homa.customer.domain.exception.SignInAlreadyHoldsAccountException;
import com.homa.customer.domain.exception.TermsOfServiceNotAcceptedException;
import com.homa.customer.usecase.dto.RegisterAccountRequest;
import com.homa.customer.usecase.dto.RegisterAccountResponse;
import com.homa.ie.events.CustomerRegisteredIE;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CustomerRegisterAccountTest {

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final RecordingCustomerIEPublisher publisher = new RecordingCustomerIEPublisher();
    private final CustomerRegisterAccount registerAccount = new CustomerRegisterAccount(customers, publisher,
            new FixedTimeSource(REGISTERED_AT), new SequentialEventIds());

    @Test
    @Tag("CUS-1")
    @Tag("CUS-7")
    @DisplayName("Aisyah signs in with Google and enters her name, her Salak Tinggi address and \"Guardhouse: say "
            + "unit B-12-3\". No phone number is asked for.")
    void customerRegistersWithHerGoogleAccountAndIsAnnounced() {
        RegisterAccountResponse response = registerAccount.register(aisyahRegisters());

        Customer aisyah = customers.get(response.customerId());
        assertThat(customers.saves()).containsExactly(aisyah.id());
        assertThat(aisyah.signInId()).isEqualTo(new SignInId(GOOGLE, AISYAH_GOOGLE_SUBJECT));
        assertThat(aisyah.name().value()).isEqualTo(AISYAH_NAME);
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo(AISYAH_STREET_ADDRESS);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo(AISYAH_CITY);
        assertThat(aisyah.arrivalNote().value()).isEqualTo(AISYAH_ARRIVAL_NOTE);
        assertThat(aisyah.verifiedIdentity()).isEmpty();
        assertThat(response.status()).isEqualTo("ACTIVE");

        assertThat(publisher.published()).hasSize(1);
        CustomerRegisteredIE registered = (CustomerRegisteredIE) publisher.published().getFirst();
        assertThat(registered.eventId()).isEqualTo("event-1");
        assertThat(registered.customerId()).isEqualTo(response.customerId());
        assertThat(registered.city()).isEqualTo(AISYAH_CITY);
        assertThat(registered.streetAddress()).isEqualTo(AISYAH_STREET_ADDRESS);
        assertThat(registered.arrivalNote()).isEqualTo(AISYAH_ARRIVAL_NOTE);
        assertThat(registered.status()).isEqualTo("ACTIVE");
        assertThat(registered.occurredAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    @Tag("CUS-2")
    @DisplayName("Aisyah ticks the Terms of Service when she registers, and Homa keeps when she agreed.")
    void registrationKeepsWhenTheCustomerAgreedToTheTerms() {
        RegisterAccountResponse response = registerAccount.register(aisyahRegisters());

        assertThat(customers.get(response.customerId()).termsAcceptance().acceptedAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    @Tag("CUS-2")
    @DisplayName("Aisyah can't finish registering until she ticks the Terms of Service.")
    void customerCantRegisterWithoutAgreeingToTheTerms() {
        assertThatThrownBy(() -> registerAccount.register(aisyahRegisters(false)))
                .isInstanceOf(TermsOfServiceNotAcceptedException.class);

        assertNothingRegistered();
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("Daniel can't open a second account with the same Google account.")
    void aSignInThatHoldsAnAccountCantRegisterAnother() {
        registerAccount.register(aisyahRegisters());

        assertThatThrownBy(() -> registerAccount.register(aisyahRegisters()))
                .isInstanceOf(SignInAlreadyHoldsAccountException.class);

        assertThat(customers.all()).hasSize(1);
        assertThat(customers.saves()).hasSize(1);
        assertThat(publisher.published()).hasSize(1);
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("The same account id at another identity provider is another sign-in, so it can hold its own "
            + "account.")
    void theSameSubjectAtAnotherIdentityProviderIsAnotherSignIn() {
        registerAccount.register(aisyahRegisters());

        RegisterAccountResponse second = registerAccount.register(new RegisterAccountRequest(APPLE,
                AISYAH_GOOGLE_SUBJECT, "Nadia", "7, Jalan Salak 2", AISYAH_CITY, "Green door", true));

        assertThat(customers.get(second.customerId()).signInId())
                .isEqualTo(new SignInId(APPLE, AISYAH_GOOGLE_SUBJECT));
        assertThat(customers.all()).hasSize(2);
        assertThat(publisher.published()).extracting(event -> event.eventId())
                .containsExactly("event-1", "event-2");
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("A customer signs in through an identity provider; a registration without its account id is "
            + "refused.")
    void registrationNeedsASignIn() {
        assertThatThrownBy(() -> registerAccount.register(new RegisterAccountRequest(GOOGLE, " ", AISYAH_NAME,
                AISYAH_STREET_ADDRESS, AISYAH_CITY, AISYAH_ARRIVAL_NOTE, true)))
                .isInstanceOf(MissingSignInException.class);

        assertNothingRegistered();
    }

    static Stream<Arguments> registrationsMissingADetail() {
        return Stream.of(
                Arguments.of("a name", new RegisterAccountRequest(GOOGLE, AISYAH_GOOGLE_SUBJECT, " ",
                        AISYAH_STREET_ADDRESS, AISYAH_CITY, AISYAH_ARRIVAL_NOTE, true)),
                Arguments.of("a home address", new RegisterAccountRequest(GOOGLE, AISYAH_GOOGLE_SUBJECT,
                        AISYAH_NAME, null, AISYAH_CITY, AISYAH_ARRIVAL_NOTE, true)),
                Arguments.of("a city in the home address", new RegisterAccountRequest(GOOGLE,
                        AISYAH_GOOGLE_SUBJECT, AISYAH_NAME, AISYAH_STREET_ADDRESS, "", AISYAH_ARRIVAL_NOTE, true)),
                Arguments.of("an arrival note", new RegisterAccountRequest(GOOGLE, AISYAH_GOOGLE_SUBJECT,
                        AISYAH_NAME, AISYAH_STREET_ADDRESS, AISYAH_CITY, null, true)));
    }

    @ParameterizedTest(name = "without {0}")
    @MethodSource("registrationsMissingADetail")
    @Tag("CUS-1")
    @DisplayName("A customer registers with a name, a home address with its city, and an arrival note.")
    void customerCantRegisterWithoutEveryDetail(String detail, RegisterAccountRequest request) {
        ThrowingCallable registering = () -> registerAccount.register(request);

        assertThatThrownBy(registering)
                .isInstanceOfSatisfying(MissingCustomerDetailException.class,
                        missing -> assertThat(missing.detail()).isEqualTo(detail));

        assertNothingRegistered();
    }

    private void assertNothingRegistered() {
        assertThat(customers.all()).isEmpty();
        assertThat(customers.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }
}
