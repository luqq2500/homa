package account.usecase;

import static account.usecase.AccountUsecaseFixtures.CLOCK;
import static account.usecase.AccountUsecaseFixtures.CODE;
import static account.usecase.AccountUsecaseFixtures.NOW;
import static account.usecase.AccountUsecaseFixtures.PHONE;
import static account.usecase.AccountUsecaseFixtures.SARAHS_STREET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.BusinessRegistration;
import account.domain.KybResult;
import account.domain.ProviderAccount;
import account.usecase.AccountUsecaseFixtures.FixedCodes;
import account.usecase.AccountUsecaseFixtures.InMemoryCustomers;
import account.usecase.AccountUsecaseFixtures.InMemoryPhoneVerifications;
import account.usecase.AccountUsecaseFixtures.InMemoryProviders;
import account.usecase.AccountUsecaseFixtures.KnownStreets;
import account.usecase.AccountUsecaseFixtures.KycProvider;
import account.usecase.AccountUsecaseFixtures.PublishedEvents;
import account.usecase.AccountUsecaseFixtures.SentSms;
import account.usecase.AccountUsecaseFixtures.StripeOnboarding;
import account.usecase.dto.AddPayoutAccountRequest;
import account.usecase.dto.AddPayoutAccountResponse;
import account.usecase.dto.ChangeAddressRequest;
import account.usecase.dto.ChangeAddressResponse;
import account.usecase.dto.ConfirmPhoneRequest;
import account.usecase.dto.CustomerViewAccountRequest;
import account.usecase.dto.CustomerViewAccountResponse;
import account.usecase.dto.ProviderViewAccountRequest;
import account.usecase.dto.ProviderViewAccountResponse;
import account.usecase.dto.RecordPayoutAccountReadyRequest;
import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterProviderRequest;
import account.usecase.dto.StartKybRequest;
import account.usecase.dto.StartKybResponse;
import account.usecase.dto.StartPhoneVerificationRequest;
import account.usecase.exception.AddressNotFoundException;
import account.usecase.exception.ChangedMeanwhileException;
import account.usecase.exception.CustomerNotFoundException;
import account.usecase.exception.ProviderNotFoundException;
import ie.events.PayoutAccountReadyIE;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** What customers and businesses do with their accounts after registering. */
class AccountsTest {

    private final InMemoryPhoneVerifications verifications = new InMemoryPhoneVerifications();
    private final InMemoryCustomers customers = new InMemoryCustomers();
    private final InMemoryProviders providers = new InMemoryProviders();
    private final PublishedEvents events = new PublishedEvents();
    private final KnownStreets geocoder = new KnownStreets();

    private String aConfirmedVerification() {
        String id = new RegistrantStartPhoneVerification(verifications, new FixedCodes(), new SentSms(), CLOCK)
                .start(new StartPhoneVerificationRequest(PHONE)).verificationId();
        new RegistrantConfirmPhone(verifications, CLOCK).confirm(new ConfirmPhoneRequest(id, CODE));
        return id;
    }

    @Nested
    class TheCustomer {

        private final String sarah = new CustomerRegisterAccount(verifications, customers, geocoder, new PublishedEvents(), CLOCK)
                .register(new RegisterCustomerRequest(aConfirmedVerification(), "Sarah Lim", SARAHS_STREET)).customerId();
        private final CustomerChangeAddress changeAddress = new CustomerChangeAddress(customers, geocoder);
        private final CustomerViewAccount view = new CustomerViewAccount(customers);

        @Test
        void aCustomerSeesTheirNamePhoneAndAddress() {
            CustomerViewAccountResponse account = view.view(new CustomerViewAccountRequest(sarah));

            assertThat(account).isEqualTo(new CustomerViewAccountResponse(sarah, "Sarah Lim", PHONE, SARAHS_STREET, "Cyberjaya"));
            assertThat(account.customerId()).isEqualTo(sarah);
            assertThat(account.name()).isEqualTo("Sarah Lim");
            assertThat(account.phone()).isEqualTo(PHONE);
            assertThat(account.street()).isEqualTo(SARAHS_STREET);
            assertThat(account.city()).isEqualTo("Cyberjaya");
        }

        @Test
        void aChangedAddressIsSavedWithTheCityTheGeoserviceFinds() {
            int savesBefore = customers.saves;

            ChangeAddressResponse changed = changeAddress.change(new ChangeAddressRequest(sarah, "8 Persiaran Perdana"));

            assertThat(changed).isEqualTo(new ChangeAddressResponse("8 Persiaran Perdana", "Putrajaya"));
            assertThat(changed.street()).isEqualTo("8 Persiaran Perdana");
            assertThat(changed.city()).isEqualTo("Putrajaya");
            assertThat(customers.saved.get(sarah).address().city().name()).isEqualTo("Putrajaya");
            assertThat(customers.saves).isEqualTo(savesBefore + 1);
        }

        @Test
        void anAddressTheGeoserviceCannotFindLeavesTheOldOne() {
            assertThatThrownBy(() -> changeAddress.change(new ChangeAddressRequest(sarah, "Nowhere Lane")))
                    .isInstanceOf(AddressNotFoundException.class);

            assertThat(customers.saved.get(sarah).address().street()).isEqualTo(SARAHS_STREET);
        }

        @Test
        void aCustomerThatDoesNotExistHasNoAccountToSeeOrChange() {
            assertThatThrownBy(() -> view.view(new CustomerViewAccountRequest("no-such-customer")))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("There is no customer no-such-customer");
            assertThatThrownBy(() -> changeAddress.change(new ChangeAddressRequest("no-such-customer", SARAHS_STREET)))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    class TheBusiness {

        private final String kemas = new ProviderRegisterAccount(verifications, providers)
                .register(new RegisterProviderRequest(aConfirmedVerification())).providerId();
        private final KycProvider kyc = new KycProvider();
        private final StripeOnboarding stripe = new StripeOnboarding();
        private final StripeRecordPayoutAccountReady payoutReady = new StripeRecordPayoutAccountReady(providers, events, CLOCK);
        private final ProviderViewAccount view = new ProviderViewAccount(providers);

        private void kybPasses() {
            ProviderAccount provider = providers.saved.get(kemas);
            provider.recordKybResult(
                    KybResult.verified(new BusinessRegistration("202601012345", "Kemas Home Care Sdn Bhd"), Instant.parse("2027-11-12T00:00:00Z")),
                    NOW);
        }

        @Test
        void aBusinessOpensTheKycProvidersKybPages() {
            StartKybResponse started = new ProviderStartKyb(providers, kyc).start(new StartKybRequest(kemas));

            assertThat(started.kybUrl()).isEqualTo("https://kyc.example/kyb/" + kemas);
            assertThat(kyc.opened).containsExactly("kyb " + kemas);
            assertThat(providers.saves).isEqualTo(1);
        }

        @Test
        void aBusinessOpensStripesOnboardingForItsPayoutAccount() {
            AddPayoutAccountResponse started = new ProviderAddPayoutAccount(providers, stripe).add(new AddPayoutAccountRequest(kemas));

            assertThat(started.onboardingUrl()).isEqualTo("https://stripe.example/onboarding/" + kemas);
            assertThat(stripe.opened).containsExactly(kemas);
        }

        @Test
        void aBusinessThatDoesNotExistOpensNothing() {
            assertThatThrownBy(() -> new ProviderStartKyb(providers, kyc).start(new StartKybRequest("no-such-provider")))
                    .isInstanceOf(ProviderNotFoundException.class)
                    .hasMessage("There is no provider no-such-provider");
            assertThatThrownBy(() -> new ProviderAddPayoutAccount(providers, stripe).add(new AddPayoutAccountRequest("no-such-provider")))
                    .isInstanceOf(ProviderNotFoundException.class);
            assertThat(kyc.opened).isEmpty();
            assertThat(stripe.opened).isEmpty();
        }

        @Test
        void aReadyPayoutAccountIsSavedAndToldToTheOtherContexts() {
            payoutReady.record(new RecordPayoutAccountReadyRequest(kemas));

            assertThat(providers.saved.get(kemas).isPayoutAccountReady()).isTrue();
            assertThat(providers.saves).isEqualTo(2);
            assertThat(events.all).containsExactly(new PayoutAccountReadyIE(kemas, NOW));
        }

        @Test
        void aRepeatedReportFromStripeSavesAndTellsNothing() {
            payoutReady.record(new RecordPayoutAccountReadyRequest(kemas));

            payoutReady.record(new RecordPayoutAccountReadyRequest(kemas));

            assertThat(providers.saves).isEqualTo(2);
            assertThat(events.all).hasSize(1);
        }

        @Test
        void nothingIsToldWhenTheReadyAccountCannotBeSaved() {
            providers.somethingElseSavesFirst();

            assertThatThrownBy(() -> payoutReady.record(new RecordPayoutAccountReadyRequest(kemas)))
                    .isInstanceOf(ChangedMeanwhileException.class)
                    .hasMessage("The provider account changed meanwhile; try again on what is there now");
            assertThat(events.all).isEmpty();
        }

        @Test
        void aNewBusinessSeesItIsWaitingForKybAndNotReady() {
            ProviderViewAccountResponse account = view.view(new ProviderViewAccountRequest(kemas));

            assertThat(account).isEqualTo(new ProviderViewAccountResponse(kemas, PHONE, "PENDING", "", "", "", false, false));
        }

        @Test
        void aVerifiedBusinessWithAReadyPayoutAccountSeesItsRegistrationAndThatItIsReady() {
            kybPasses();
            payoutReady.record(new RecordPayoutAccountReadyRequest(kemas));

            ProviderViewAccountResponse account = view.view(new ProviderViewAccountRequest(kemas));

            assertThat(account.providerId()).isEqualTo(kemas);
            assertThat(account.phone()).isEqualTo(PHONE);
            assertThat(account.verification()).isEqualTo("VERIFIED");
            assertThat(account.ssmNumber()).isEqualTo("202601012345");
            assertThat(account.registeredName()).isEqualTo("Kemas Home Care Sdn Bhd");
            assertThat(account.verifiedUntil()).isEqualTo("2027-11-12T00:00:00Z");
            assertThat(account.payoutAccountReady()).isTrue();
            assertThat(account.readyForGoLive()).isTrue();
        }

        @Test
        void aVerifiedBusinessWithoutAPayoutAccountIsNotReady() {
            kybPasses();

            ProviderViewAccountResponse account = view.view(new ProviderViewAccountRequest(kemas));

            assertThat(account.payoutAccountReady()).isFalse();
            assertThat(account.readyForGoLive()).isFalse();
        }

        @Test
        void aBusinessThatDoesNotExistHasNoAccountToSee() {
            assertThatThrownBy(() -> view.view(new ProviderViewAccountRequest("no-such-provider")))
                    .isInstanceOf(ProviderNotFoundException.class);
            assertThatThrownBy(() -> payoutReady.record(new RecordPayoutAccountReadyRequest("no-such-provider")))
                    .isInstanceOf(ProviderNotFoundException.class);
        }
    }
}
