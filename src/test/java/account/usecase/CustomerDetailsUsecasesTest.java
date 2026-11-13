package account.usecase;

import static account.support.AccountFixtures.NOW;
import static account.support.AccountFixtures.PIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.support.AccountWorld;
import account.usecase.dto.ChangeAccessNotesRequest;
import account.usecase.dto.ChangeAccessNotesResponse;
import account.usecase.dto.ChangeAddressRequest;
import account.usecase.dto.ChangeAddressResponse;
import account.usecase.dto.ChangeNameRequest;
import account.usecase.dto.ChangeNameResponse;
import account.usecase.dto.ChooseBusinessLanguageRequest;
import account.usecase.dto.ChooseBusinessLanguageResponse;
import account.usecase.dto.ChooseCustomerLanguageRequest;
import account.usecase.dto.ChooseCustomerLanguageResponse;
import account.usecase.exception.AccountNotFoundException;
import account.usecase.exception.AddressOutsideHomaException;
import account.usecase.exception.UnknownLanguageException;
import ie.events.BusinessLanguageChosenIE;
import ie.events.CustomerDetailsChangedIE;
import java.util.Optional;
import kernel.City;
import kernel.GeoPoint;
import org.junit.jupiter.api.Test;

class CustomerDetailsUsecasesTest {

    private final AccountWorld world = new AccountWorld();
    private final String customerId = world.customer("0123456789");

    private CustomerDetailsChangedIE details(String name, String line, GeoPoint pin, City city, String notes,
                                             String language) {
        return new CustomerDetailsChangedIE(customerId, name, line, pin, city, notes, language, NOW);
    }

    @Test
    void aChangedNameIsSavedAndTheOtherContextsAreTold() {
        ChangeNameResponse response = new CustomerChangeName(world.customers, world.publisher, world.clock)
                .change(new ChangeNameRequest(customerId, "Aisyah Rahman"));

        assertThat(response).isEqualTo(new ChangeNameResponse(customerId, "Aisyah Rahman"));
        assertThat(world.customers.timesSaved(customerId)).isEqualTo(2);
        assertThat(world.publisher.published(CustomerDetailsChangedIE.class)).containsExactly(
                details("Aisyah Rahman", "12 Jalan Teknokrat 5", PIN, City.CYBERJAYA, "", "ENGLISH"));
    }

    @Test
    void aChangedAddressTakesTheCityOfItsNewPin() {
        world.cityOfPins = Optional.of(City.SALAK_TINGGI);
        CustomerChangeAddress usecase =
                new CustomerChangeAddress(world.customers, world.cities, world.publisher, world.clock);

        ChangeAddressResponse response = usecase.change(new ChangeAddressRequest(customerId, "8 Jalan ST 1", 2.81, 101.72));

        assertThat(response).isEqualTo(new ChangeAddressResponse(customerId, "SALAK_TINGGI"));
        assertThat(world.customers.timesSaved(customerId)).isEqualTo(2);
        assertThat(world.publisher.published(CustomerDetailsChangedIE.class)).containsExactly(details(
                "Aisyah binti Rahman", "8 Jalan ST 1", new GeoPoint(2.81, 101.72), City.SALAK_TINGGI, "", "ENGLISH"));
    }

    @Test
    void anAddressOutsideHomasCitiesIsNotSaved() {
        world.cityOfPins = Optional.empty();
        CustomerChangeAddress usecase =
                new CustomerChangeAddress(world.customers, world.cities, world.publisher, world.clock);

        assertThatThrownBy(() -> usecase.change(new ChangeAddressRequest(customerId, "1 Jalan Ampang", 3.15, 101.71)))
                .isInstanceOf(AddressOutsideHomaException.class);
        assertThat(world.customers.find(customerId).orElseThrow().address().city()).isEqualTo(City.CYBERJAYA);
        assertThat(world.publisher.published(CustomerDetailsChangedIE.class)).isEmpty();
    }

    @Test
    void changedAccessNotesAreSavedForNewRequests() {
        ChangeAccessNotesResponse response =
                new CustomerChangeAccessNotes(world.customers, world.publisher, world.clock)
                        .change(new ChangeAccessNotesRequest(customerId, "Unit 12-3, register at the guardhouse"));

        assertThat(response)
                .isEqualTo(new ChangeAccessNotesResponse(customerId, "Unit 12-3, register at the guardhouse"));
        assertThat(world.customers.timesSaved(customerId)).isEqualTo(2);
        assertThat(world.publisher.published(CustomerDetailsChangedIE.class)).containsExactly(details(
                "Aisyah binti Rahman", "12 Jalan Teknokrat 5", PIN, City.CYBERJAYA,
                "Unit 12-3, register at the guardhouse", "ENGLISH"));
    }

    @Test
    void aCustomerChoosesTheLanguageOfTheirApp() {
        CustomerChooseLanguage usecase = new CustomerChooseLanguage(world.customers, world.publisher, world.clock);

        ChooseCustomerLanguageResponse response = usecase.choose(new ChooseCustomerLanguageRequest(customerId, "MALAY"));

        assertThat(response).isEqualTo(new ChooseCustomerLanguageResponse(customerId, "MALAY"));
        assertThat(world.customers.timesSaved(customerId)).isEqualTo(2);
        assertThat(world.publisher.published(CustomerDetailsChangedIE.class)).containsExactly(
                details("Aisyah binti Rahman", "12 Jalan Teknokrat 5", PIN, City.CYBERJAYA, "", "MALAY"));
        assertThatThrownBy(() -> usecase.choose(new ChooseCustomerLanguageRequest(customerId, "TAMIL")))
                .isInstanceOf(UnknownLanguageException.class);
    }

    @Test
    void aBusinessChoosesTheLanguageOfItsApp() {
        String businessId = world.business("0198765432");
        BusinessChooseLanguage usecase = new BusinessChooseLanguage(world.businesses, world.publisher, world.clock);

        ChooseBusinessLanguageResponse response =
                usecase.choose(new ChooseBusinessLanguageRequest(businessId, "ENGLISH"));

        assertThat(response).isEqualTo(new ChooseBusinessLanguageResponse(businessId, "ENGLISH"));
        assertThat(world.businesses.timesSaved(businessId)).isEqualTo(2);
        assertThat(world.publisher.published(BusinessLanguageChosenIE.class))
                .containsExactly(new BusinessLanguageChosenIE(businessId, "ENGLISH", NOW));
        assertThatThrownBy(() -> usecase.choose(new ChooseBusinessLanguageRequest("none", "ENGLISH")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void onlyAnExistingCustomerCanChangeTheirDetails() {
        assertThatThrownBy(() -> new CustomerChangeName(world.customers, world.publisher, world.clock)
                .change(new ChangeNameRequest("none", "Aisyah"))).isInstanceOf(AccountNotFoundException.class);
    }
}
