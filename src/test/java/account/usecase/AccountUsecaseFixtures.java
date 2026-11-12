package account.usecase;

import account.domain.CustomerAccount;
import account.domain.PhoneVerification;
import account.domain.ProviderAccount;
import account.usecase.exception.ChangedMeanwhileException;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.Geocoder;
import account.usecase.port.out.IdentityVerificationService;
import account.usecase.port.out.Location;
import account.usecase.port.out.PayoutAccountService;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.ProviderAccountRepository;
import account.usecase.port.out.SmsSender;
import account.usecase.port.out.VerificationCodes;
import ie.port.AccountIE;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory stand-ins for the account context's out ports, and the values the usecase tests share. */
final class AccountUsecaseFixtures {

    /** Thursday 12 November 2026, 09:00 in Malaysia. */
    static final Instant NOW = Instant.parse("2026-11-12T01:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final String PHONE = "+60123456789";
    static final String CODE = "482915";
    static final String SARAHS_STREET = "12 Jalan Teknokrat 3";

    private AccountUsecaseFixtures() {
    }

    static Clock at(Instant now) {
        return Clock.fixed(now, ZoneOffset.UTC);
    }

    /** Refuses the next save, as a repository does when something else saved first. */
    abstract static class Refusable {
        private boolean refuseNext;

        void somethingElseSavesFirst() {
            refuseNext = true;
        }

        void refuseIfChangedMeanwhile(String what) {
            if (refuseNext) {
                refuseNext = false;
                throw new ChangedMeanwhileException(what);
            }
        }
    }

    static final class InMemoryPhoneVerifications extends Refusable implements PhoneVerificationRepository {
        final Map<String, PhoneVerification> saved = new HashMap<>();
        int saves;

        @Override
        public Optional<PhoneVerification> find(String verificationId) {
            return Optional.ofNullable(saved.get(verificationId));
        }

        @Override
        public void save(PhoneVerification verification) {
            refuseIfChangedMeanwhile("The phone verification");
            saved.put(verification.id(), verification);
            saves++;
        }

        @Override
        public void remove(String verificationId) {
            saved.remove(verificationId);
        }
    }

    static final class InMemoryCustomers extends Refusable implements CustomerAccountRepository {
        final Map<String, CustomerAccount> saved = new HashMap<>();
        int saves;

        @Override
        public Optional<CustomerAccount> find(String customerId) {
            return Optional.ofNullable(saved.get(customerId));
        }

        @Override
        public boolean existsWithPhone(String phone) {
            return saved.values().stream().anyMatch(account -> account.phone().value().equals(phone));
        }

        @Override
        public void save(CustomerAccount account) {
            refuseIfChangedMeanwhile("The customer account");
            saved.put(account.id(), account);
            saves++;
        }
    }

    static final class InMemoryProviders extends Refusable implements ProviderAccountRepository {
        final Map<String, ProviderAccount> saved = new HashMap<>();
        int saves;

        @Override
        public Optional<ProviderAccount> find(String providerId) {
            return Optional.ofNullable(saved.get(providerId));
        }

        @Override
        public boolean existsWithPhone(String phone) {
            return saved.values().stream().anyMatch(account -> account.phone().value().equals(phone));
        }

        @Override
        public void save(ProviderAccount account) {
            refuseIfChangedMeanwhile("The provider account");
            saved.put(account.id(), account);
            saves++;
        }
    }

    static final class PublishedEvents implements AccountIEPublisher {
        final List<AccountIE> all = new ArrayList<>();

        @Override
        public void publish(AccountIE event) {
            all.add(event);
        }
    }

    /** Keeps every SMS sent, as "phone code". */
    static final class SentSms implements SmsSender {
        final List<String> all = new ArrayList<>();

        @Override
        public void sendVerificationCode(String phone, String code) {
            all.add(phone + " " + code);
        }
    }

    /** Always makes the same code, so a test can enter it. */
    static final class FixedCodes implements VerificationCodes {
        @Override
        public String newCode() {
            return CODE;
        }
    }

    /** Finds the streets it was told about. */
    static final class KnownStreets implements Geocoder {
        final Map<String, Location> streets = new HashMap<>(Map.of(
                SARAHS_STREET, new Location("Cyberjaya", 2.9213, 101.6559),
                "8 Persiaran Perdana", new Location("Putrajaya", 2.9264, 101.6964)));

        @Override
        public Optional<Location> locate(String street) {
            return Optional.ofNullable(streets.get(street));
        }
    }

    /** The KYC provider: returns a page address for each check it opens, and keeps what it was asked. */
    static final class KycProvider implements IdentityVerificationService {
        final List<String> opened = new ArrayList<>();

        @Override
        public String startKyb(String providerId) {
            opened.add("kyb " + providerId);
            return "https://kyc.example/kyb/" + providerId;
        }
    }

    static final class StripeOnboarding implements PayoutAccountService {
        final List<String> opened = new ArrayList<>();

        @Override
        public String startOnboarding(String providerId) {
            opened.add(providerId);
            return "https://stripe.example/onboarding/" + providerId;
        }
    }
}
