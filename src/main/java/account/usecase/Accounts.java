package account.usecase;

import account.domain.Address;
import account.domain.BusinessAccount;
import account.domain.CustomerAccount;
import account.domain.Language;
import account.usecase.exception.AccountNotFoundException;
import account.usecase.exception.AddressOutsideHomaException;
import account.usecase.exception.UnknownLanguageException;
import account.usecase.port.out.BusinessAccountRepository;
import account.usecase.port.out.CityLocator;
import account.usecase.port.out.CustomerAccountRepository;
import ie.events.CustomerDetailsChangedIE;
import ie.events.CustomerRegisteredIE;
import java.time.Instant;
import kernel.City;
import kernel.GeoPoint;

/** What the account usecases share: looking accounts up, reading what was typed, and building events. */
final class Accounts {

    private Accounts() {
    }

    static CustomerAccount customer(CustomerAccountRepository customers, String customerId) {
        return customers.find(customerId).orElseThrow(() -> new AccountNotFoundException(customerId));
    }

    static BusinessAccount business(BusinessAccountRepository businesses, String businessId) {
        return businesses.find(businessId).orElseThrow(() -> new AccountNotFoundException(businessId));
    }

    static Language language(String name) {
        try {
            return Language.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownLanguageException(name);
        }
    }

    /** The address with its pin, in the city the geoservice finds the pin in. */
    static Address address(CityLocator cities, String line, double latitude, double longitude) {
        GeoPoint pin = new GeoPoint(latitude, longitude);
        City city = cities.cityAt(pin).orElseThrow(AddressOutsideHomaException::new);
        return new Address(line, pin, city);
    }

    static CustomerRegisteredIE registered(CustomerAccount account, Instant now) {
        Address address = account.address();
        return new CustomerRegisteredIE(account.id(), account.name(), address.line(), address.pin(), address.city(),
                account.accessNotes(), account.language().name(), now);
    }

    static CustomerDetailsChangedIE detailsChanged(CustomerAccount account, Instant now) {
        Address address = account.address();
        return new CustomerDetailsChangedIE(account.id(), account.name(), address.line(), address.pin(),
                address.city(), account.accessNotes(), account.language().name(), now);
    }
}
