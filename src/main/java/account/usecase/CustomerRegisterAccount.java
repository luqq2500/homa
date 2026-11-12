package account.usecase;

import account.domain.Address;
import account.domain.CustomerAccount;
import account.domain.PhoneVerification;
import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterCustomerResponse;
import account.usecase.exception.PhoneAlreadyRegisteredException;
import account.usecase.port.in.CustomerAccountRegistrator;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.Geocoder;
import account.usecase.port.out.PhoneVerificationRepository;
import ie.events.CustomerRegisteredIE;
import java.time.Clock;

/**
 * Registers a customer with a confirmed phone number, their name and their address. The address is found
 * by the geoservice, for its city and coordinates. The verification is used up, so it registers no other
 * account.
 */
public final class CustomerRegisterAccount implements CustomerAccountRegistrator {

    private final PhoneVerificationRepository verifications;
    private final CustomerAccountRepository customers;
    private final Geocoder geocoder;
    private final AccountIEPublisher events;
    private final Clock clock;

    public CustomerRegisterAccount(
            PhoneVerificationRepository verifications,
            CustomerAccountRepository customers,
            Geocoder geocoder,
            AccountIEPublisher events,
            Clock clock) {
        this.verifications = verifications;
        this.customers = customers;
        this.geocoder = geocoder;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public RegisterCustomerResponse register(RegisterCustomerRequest request) {
        PhoneVerification verified = Accounts.verification(verifications, request.verificationId());
        if (customers.existsWithPhone(verified.phone().value())) {
            throw new PhoneAlreadyRegisteredException("customer");
        }
        Address address = Addresses.locate(geocoder, request.street());

        CustomerAccount customer = CustomerAccount.register(verified, request.name(), address);

        customers.save(customer);
        verifications.remove(verified.id());
        events.publish(new CustomerRegisteredIE(customer.id(), customer.name(), address.city().name(), clock.instant()));
        return new RegisterCustomerResponse(customer.id(), customer.name(), address.street(), address.city().name());
    }
}
