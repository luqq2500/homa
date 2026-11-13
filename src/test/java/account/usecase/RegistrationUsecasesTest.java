package account.usecase;

import static account.support.AccountFixtures.NOW;
import static account.support.AccountFixtures.PIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.CustomerAccount;
import account.domain.Language;
import account.domain.exception.PhoneNotVerifiedException;
import account.support.AccountWorld;
import account.usecase.dto.RegisterBusinessRequest;
import account.usecase.dto.RegisterBusinessResponse;
import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterCustomerResponse;
import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.exception.AddressOutsideHomaException;
import account.usecase.exception.PhoneAlreadyRegisteredException;
import account.usecase.exception.PhoneVerificationNotFoundException;
import account.usecase.exception.UnknownLanguageException;
import ie.events.BusinessRegisteredIE;
import ie.events.CustomerRegisteredIE;
import java.util.Optional;
import kernel.City;
import org.junit.jupiter.api.Test;

class RegistrationUsecasesTest {

    private final AccountWorld world = new AccountWorld();
    private final GuestRegisterCustomer customerRegistrator = new GuestRegisterCustomer(
            world.verifications, world.customers, world.cities, world.publisher, world.clock);
    private final GuestRegisterBusiness businessRegistrator =
            new GuestRegisterBusiness(world.verifications, world.businesses, world.publisher, world.clock);

    private RegisterCustomerRequest customerRequest(String verificationId) {
        return new RegisterCustomerRequest(verificationId, "Aisyah binti Rahman", "12 Jalan Teknokrat 5",
                PIN.latitude(), PIN.longitude(), "MALAY");
    }

    @Test
    void aCustomerRegistersWithNameAndAddressInTheCityOfTheirPin() {
        world.cityOfPins = Optional.of(City.PUTRAJAYA);
        String verificationId = world.verifiedPhone("0123456789");

        RegisterCustomerResponse response = customerRegistrator.register(customerRequest(verificationId));

        assertThat(response.city()).isEqualTo("PUTRAJAYA");
        CustomerAccount account = world.customers.find(response.customerId()).orElseThrow();
        assertThat(account.phone()).isEqualTo(world.phone("0123456789"));
        assertThat(account.name()).isEqualTo("Aisyah binti Rahman");
        assertThat(account.address().city()).isEqualTo(City.PUTRAJAYA);
        assertThat(account.address().pin()).isEqualTo(PIN);
        assertThat(account.language()).isEqualTo(Language.MALAY);
        assertThat(world.verifications.find(verificationId).orElseThrow().isUsed()).isTrue();
        assertThat(world.verifications.timesSaved(verificationId)).isEqualTo(3);
        assertThat(world.publisher.published()).containsExactly(new CustomerRegisteredIE(response.customerId(),
                "Aisyah binti Rahman", "12 Jalan Teknokrat 5", PIN, City.PUTRAJAYA, "", "MALAY", NOW));
    }

    @Test
    void aPhoneNumberHasAtMostOneCustomerAccount() {
        world.customer("0123456789");
        String again = world.verifiedPhone("0123456789");

        assertThatThrownBy(() -> customerRegistrator.register(customerRequest(again)))
                .isInstanceOf(PhoneAlreadyRegisteredException.class);
        assertThat(world.customers.all()).hasSize(1);
        assertThat(world.verifications.find(again).orElseThrow().isUsed()).isFalse();
    }

    @Test
    void theSamePersonCanHaveOneCustomerAndOneBusinessAccount() {
        String customerId = world.customer("0123456789");
        String businessId = world.business("0123456789");

        assertThat(world.customers.find(customerId)).isPresent();
        assertThat(world.businesses.find(businessId)).isPresent();
    }

    @Test
    void aCustomerCannotRegisterWithoutConfirmingTheCode() {
        String unconfirmed = new GuestRequestPhoneCode(world.verifications, world.verifier)
                .request(new RequestPhoneCodeRequest("0123456789")).verificationId();

        assertThatThrownBy(() -> customerRegistrator.register(customerRequest(unconfirmed)))
                .isInstanceOf(PhoneNotVerifiedException.class);
        assertThatThrownBy(() -> customerRegistrator.register(customerRequest("none")))
                .isInstanceOf(PhoneVerificationNotFoundException.class);
        assertThat(world.customers.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    void aCustomerCannotRegisterAnAddressOutsideHomasCities() {
        world.cityOfPins = Optional.empty();
        String verificationId = world.verifiedPhone("0123456789");

        assertThatThrownBy(() -> customerRegistrator.register(customerRequest(verificationId)))
                .isInstanceOf(AddressOutsideHomaException.class);
        assertThat(world.customers.all()).isEmpty();
        assertThat(world.verifications.find(verificationId).orElseThrow().isUsed()).isFalse();
    }

    @Test
    void theAppIsOnlyInMalayAndEnglish() {
        String verificationId = world.verifiedPhone("0123456789");

        assertThatThrownBy(() -> customerRegistrator.register(new RegisterCustomerRequest(verificationId, "Aisyah",
                "12 Jalan", PIN.latitude(), PIN.longitude(), "FRENCH"))).isInstanceOf(UnknownLanguageException.class);
        assertThatThrownBy(() -> businessRegistrator.register(new RegisterBusinessRequest(verificationId, null)))
                .isInstanceOf(UnknownLanguageException.class);
        assertThat(world.verifications.find(verificationId).orElseThrow().isUsed()).isFalse();
    }

    @Test
    void aBusinessRegistersWithItsPhoneNumber() {
        String verificationId = world.verifiedPhone("0123456789");

        RegisterBusinessResponse response =
                businessRegistrator.register(new RegisterBusinessRequest(verificationId, "ENGLISH"));

        assertThat(world.businesses.find(response.businessId()).orElseThrow().phone())
                .isEqualTo(world.phone("0123456789"));
        assertThat(world.verifications.timesSaved(verificationId)).isEqualTo(3);
        assertThat(world.publisher.published())
                .containsExactly(new BusinessRegisteredIE(response.businessId(), "ENGLISH", NOW));
    }

    @Test
    void aPhoneNumberHasAtMostOneBusinessAccount() {
        world.business("0123456789");
        String again = world.verifiedPhone("0123456789");

        assertThatThrownBy(() -> businessRegistrator.register(new RegisterBusinessRequest(again, "MALAY")))
                .isInstanceOf(PhoneAlreadyRegisteredException.class);
        assertThatThrownBy(() -> businessRegistrator.register(new RegisterBusinessRequest("none", "MALAY")))
                .isInstanceOf(PhoneVerificationNotFoundException.class);
        assertThat(world.businesses.all()).hasSize(1);
    }
}
