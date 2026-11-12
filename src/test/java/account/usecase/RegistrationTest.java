package account.usecase;

import static account.usecase.AccountUsecaseFixtures.CLOCK;
import static account.usecase.AccountUsecaseFixtures.CODE;
import static account.usecase.AccountUsecaseFixtures.NOW;
import static account.usecase.AccountUsecaseFixtures.PHONE;
import static account.usecase.AccountUsecaseFixtures.SARAHS_STREET;
import static account.usecase.AccountUsecaseFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.CustomerAccount;
import account.domain.ProviderAccount;
import account.domain.VerificationStatus;
import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.InvalidPhoneNumberException;
import account.domain.exception.PhoneNotVerifiedException;
import account.domain.exception.TooManyVerificationAttemptsException;
import account.domain.exception.VerificationCodeExpiredException;
import account.usecase.AccountUsecaseFixtures.FixedCodes;
import account.usecase.AccountUsecaseFixtures.InMemoryCustomers;
import account.usecase.AccountUsecaseFixtures.InMemoryPhoneVerifications;
import account.usecase.AccountUsecaseFixtures.InMemoryProviders;
import account.usecase.AccountUsecaseFixtures.KnownStreets;
import account.usecase.AccountUsecaseFixtures.PublishedEvents;
import account.usecase.AccountUsecaseFixtures.SentSms;
import account.usecase.dto.ConfirmPhoneRequest;
import account.usecase.dto.ConfirmPhoneResponse;
import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterCustomerResponse;
import account.usecase.dto.RegisterProviderRequest;
import account.usecase.dto.RegisterProviderResponse;
import account.usecase.dto.StartPhoneVerificationRequest;
import account.usecase.dto.StartPhoneVerificationResponse;
import account.usecase.exception.AddressNotFoundException;
import account.usecase.exception.ChangedMeanwhileException;
import account.usecase.exception.PhoneAlreadyRegisteredException;
import account.usecase.exception.PhoneVerificationNotFoundException;
import ie.events.CustomerRegisteredIE;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Customers and businesses register with a phone number confirmed by an SMS code. */
class RegistrationTest {

    private final InMemoryPhoneVerifications verifications = new InMemoryPhoneVerifications();
    private final InMemoryCustomers customers = new InMemoryCustomers();
    private final InMemoryProviders providers = new InMemoryProviders();
    private final PublishedEvents events = new PublishedEvents();
    private final SentSms sms = new SentSms();
    private final KnownStreets geocoder = new KnownStreets();
    private final RegistrantStartPhoneVerification start =
            new RegistrantStartPhoneVerification(verifications, new FixedCodes(), sms, CLOCK);
    private final RegistrantConfirmPhone confirm = new RegistrantConfirmPhone(verifications, CLOCK);
    private final CustomerRegisterAccount registerCustomer =
            new CustomerRegisterAccount(verifications, customers, geocoder, events, CLOCK);
    private final ProviderRegisterAccount registerProvider = new ProviderRegisterAccount(verifications, providers);

    private String aConfirmedVerification() {
        String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();
        confirm.confirm(new ConfirmPhoneRequest(id, CODE));
        return id;
    }

    @Nested
    class StartingAVerification {

        @Test
        void aVerificationIsSavedAndItsCodeIsSentBySms() {
            StartPhoneVerificationResponse started = start.start(new StartPhoneVerificationRequest(PHONE));

            assertThat(verifications.saved).containsKey(started.verificationId());
            assertThat(started.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
            assertThat(sms.all).containsExactly(PHONE + " " + CODE);
        }

        @Test
        void aNumberThatIsNotAMalaysianMobileIsRefusedAndNoSmsIsSent() {
            assertThatThrownBy(() -> start.start(new StartPhoneVerificationRequest("12345")))
                    .isInstanceOf(InvalidPhoneNumberException.class);

            assertThat(verifications.saved).isEmpty();
            assertThat(sms.all).isEmpty();
        }

        @Test
        void noSmsIsSentWhenTheVerificationCannotBeSaved() {
            verifications.somethingElseSavesFirst();

            assertThatThrownBy(() -> start.start(new StartPhoneVerificationRequest(PHONE)))
                    .isInstanceOf(ChangedMeanwhileException.class);

            assertThat(sms.all).isEmpty();
        }
    }

    @Nested
    class Confirming {

        @Test
        void theRightCodeConfirmsThePhone() {
            String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();

            ConfirmPhoneResponse confirmed = confirm.confirm(new ConfirmPhoneRequest(id, CODE));

            assertThat(confirmed).isEqualTo(new ConfirmPhoneResponse(id, true, 5));
            assertThat(confirmed.verificationId()).isEqualTo(id);
            assertThat(confirmed.confirmed()).isTrue();
            assertThat(confirmed.attemptsLeft()).isEqualTo(5);
            assertThat(verifications.saved.get(id).isConfirmed()).isTrue();
            assertThat(verifications.saves).isEqualTo(2);
        }

        @Test
        void aWrongCodeIsSavedAsAnAttemptAndSaysHowManyAreLeft() {
            String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();

            ConfirmPhoneResponse wrong = confirm.confirm(new ConfirmPhoneRequest(id, "000000"));

            assertThat(wrong.confirmed()).isFalse();
            assertThat(wrong.attemptsLeft()).isEqualTo(4);
            assertThat(verifications.saved.get(id).wrongAttempts()).isEqualTo(1);
            assertThat(verifications.saves).isEqualTo(2);
        }

        @Test
        void afterFiveWrongCodesANewCodeIsNeeded() {
            String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();
            for (int i = 0; i < 5; i++) {
                confirm.confirm(new ConfirmPhoneRequest(id, "000000"));
            }

            assertThatThrownBy(() -> confirm.confirm(new ConfirmPhoneRequest(id, CODE)))
                    .isInstanceOf(TooManyVerificationAttemptsException.class);
        }

        @Test
        void aCodeEnteredAfterFiveMinutesHasExpired() {
            String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();
            RegistrantConfirmPhone later = new RegistrantConfirmPhone(verifications, at(NOW.plus(Duration.ofMinutes(5))));

            assertThatThrownBy(() -> later.confirm(new ConfirmPhoneRequest(id, CODE)))
                    .isInstanceOf(VerificationCodeExpiredException.class);
        }

        @Test
        void aVerificationThatDoesNotExistIsNotConfirmed() {
            assertThatThrownBy(() -> confirm.confirm(new ConfirmPhoneRequest("no-such-verification", CODE)))
                    .isInstanceOf(PhoneVerificationNotFoundException.class)
                    .hasMessage("There is no phone verification no-such-verification");
        }
    }

    @Nested
    class RegisteringACustomer {

        @Test
        void aCustomerIsRegisteredWithTheCityTheGeoserviceFinds() {
            String id = aConfirmedVerification();

            RegisterCustomerResponse registered = registerCustomer.register(new RegisterCustomerRequest(id, "Sarah Lim", SARAHS_STREET));

            CustomerAccount saved = customers.saved.get(registered.customerId());
            assertThat(saved.name()).isEqualTo("Sarah Lim");
            assertThat(saved.phone().value()).isEqualTo(PHONE);
            assertThat(saved.address().street()).isEqualTo(SARAHS_STREET);
            assertThat(saved.address().city().name()).isEqualTo("Cyberjaya");
            assertThat(saved.address().coordinates().latitude()).isEqualTo(2.9213);
            assertThat(saved.address().coordinates().longitude()).isEqualTo(101.6559);
            assertThat(registered).isEqualTo(new RegisterCustomerResponse(saved.id(), "Sarah Lim", SARAHS_STREET, "Cyberjaya"));
            assertThat(registered.name()).isEqualTo("Sarah Lim");
            assertThat(registered.street()).isEqualTo(SARAHS_STREET);
            assertThat(registered.city()).isEqualTo("Cyberjaya");
        }

        @Test
        void aRegisteredCustomerIsToldToTheOtherContexts() {
            String id = aConfirmedVerification();

            RegisterCustomerResponse registered = registerCustomer.register(new RegisterCustomerRequest(id, "Sarah Lim", SARAHS_STREET));

            assertThat(events.all).containsExactly(new CustomerRegisteredIE(registered.customerId(), "Sarah Lim", "Cyberjaya", NOW));
        }

        @Test
        void theVerificationIsUsedUpSoItRegistersNoOtherAccount() {
            String id = aConfirmedVerification();
            registerCustomer.register(new RegisterCustomerRequest(id, "Sarah Lim", SARAHS_STREET));

            assertThat(verifications.saved).doesNotContainKey(id);
            assertThatThrownBy(() -> registerProvider.register(new RegisterProviderRequest(id)))
                    .isInstanceOf(PhoneVerificationNotFoundException.class);
        }

        @Test
        void aPhoneNumberHasOneCustomerAccount() {
            registerCustomer.register(new RegisterCustomerRequest(aConfirmedVerification(), "Sarah Lim", SARAHS_STREET));
            String again = aConfirmedVerification();

            assertThatThrownBy(() -> registerCustomer.register(new RegisterCustomerRequest(again, "Sarah L", SARAHS_STREET)))
                    .isInstanceOf(PhoneAlreadyRegisteredException.class)
                    .hasMessage("This phone number already has a customer account");

            assertThat(customers.saved).hasSize(1);
            assertThat(verifications.saved).containsKey(again);
            assertThat(events.all).hasSize(1);
        }

        @Test
        void anUnconfirmedPhoneCannotRegister() {
            String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();

            assertThatThrownBy(() -> registerCustomer.register(new RegisterCustomerRequest(id, "Sarah Lim", SARAHS_STREET)))
                    .isInstanceOf(PhoneNotVerifiedException.class);

            assertThat(customers.saved).isEmpty();
            assertThat(verifications.saved).containsKey(id);
            assertThat(events.all).isEmpty();
        }

        @Test
        void anAddressTheGeoserviceCannotFindIsRefused() {
            String id = aConfirmedVerification();

            assertThatThrownBy(() -> registerCustomer.register(new RegisterCustomerRequest(id, "Sarah Lim", "Nowhere Lane")))
                    .isInstanceOf(AddressNotFoundException.class)
                    .hasMessage("The address could not be found: Nowhere Lane");

            assertThat(customers.saved).isEmpty();
            assertThat(verifications.saved).containsKey(id);
        }

        @Test
        void aCustomerNeedsAName() {
            String id = aConfirmedVerification();

            assertThatThrownBy(() -> registerCustomer.register(new RegisterCustomerRequest(id, " ", SARAHS_STREET)))
                    .isInstanceOf(AccountDetailRequiredException.class);
            assertThat(customers.saved).isEmpty();
        }

        @Test
        void nothingIsToldAndTheVerificationIsKeptWhenTheAccountCannotBeSaved() {
            String id = aConfirmedVerification();
            customers.somethingElseSavesFirst();

            assertThatThrownBy(() -> registerCustomer.register(new RegisterCustomerRequest(id, "Sarah Lim", SARAHS_STREET)))
                    .isInstanceOf(ChangedMeanwhileException.class);

            assertThat(verifications.saved).containsKey(id);
            assertThat(events.all).isEmpty();
        }
    }

    @Nested
    class RegisteringABusiness {

        @Test
        void aBusinessIsRegisteredWaitingForKyb() {
            String id = aConfirmedVerification();

            RegisterProviderResponse registered = registerProvider.register(new RegisterProviderRequest(id));

            ProviderAccount saved = providers.saved.get(registered.providerId());
            assertThat(saved.phone().value()).isEqualTo(PHONE);
            assertThat(saved.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(registered).isEqualTo(new RegisterProviderResponse(saved.id(), PHONE));
            assertThat(registered.phone()).isEqualTo(PHONE);
            assertThat(verifications.saved).doesNotContainKey(id);
            assertThat(events.all).isEmpty();
        }

        @Test
        void aPhoneNumberHasOneProviderAccount() {
            registerProvider.register(new RegisterProviderRequest(aConfirmedVerification()));
            String again = aConfirmedVerification();

            assertThatThrownBy(() -> registerProvider.register(new RegisterProviderRequest(again)))
                    .isInstanceOf(PhoneAlreadyRegisteredException.class)
                    .hasMessage("This phone number already has a provider account");

            assertThat(providers.saved).hasSize(1);
            assertThat(verifications.saved).containsKey(again);
        }

        @Test
        void theSamePhoneCanHaveACustomerAccountAndAProviderAccount() {
            registerCustomer.register(new RegisterCustomerRequest(aConfirmedVerification(), "Nur Aina", SARAHS_STREET));

            registerProvider.register(new RegisterProviderRequest(aConfirmedVerification()));

            assertThat(customers.saved).hasSize(1);
            assertThat(providers.saved).hasSize(1);
        }

        @Test
        void anUnconfirmedPhoneCannotRegisterABusiness() {
            String id = start.start(new StartPhoneVerificationRequest(PHONE)).verificationId();

            assertThatThrownBy(() -> registerProvider.register(new RegisterProviderRequest(id)))
                    .isInstanceOf(PhoneNotVerifiedException.class);
            assertThat(providers.saved).isEmpty();
            assertThat(verifications.saved).containsKey(id);
        }

        @Test
        void theVerificationIsKeptWhenTheBusinessCannotBeSaved() {
            String id = aConfirmedVerification();
            providers.somethingElseSavesFirst();

            assertThatThrownBy(() -> registerProvider.register(new RegisterProviderRequest(id)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(verifications.saved).containsKey(id);
        }
    }
}
