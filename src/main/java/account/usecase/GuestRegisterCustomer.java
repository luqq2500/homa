package account.usecase;

import account.domain.Address;
import account.domain.CustomerAccount;
import account.domain.Language;
import account.domain.PhoneVerification;
import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterCustomerResponse;
import account.usecase.exception.PhoneAlreadyRegisteredException;
import account.usecase.exception.PhoneVerificationNotFoundException;
import account.usecase.port.in.GuestCustomerRegistrator;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CityLocator;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.PhoneVerificationRepository;
import java.time.Clock;

public final class GuestRegisterCustomer implements GuestCustomerRegistrator {

    private final PhoneVerificationRepository verifications;
    private final CustomerAccountRepository customers;
    private final CityLocator cities;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public GuestRegisterCustomer(PhoneVerificationRepository verifications, CustomerAccountRepository customers,
                                 CityLocator cities, AccountIEPublisher publisher, Clock clock) {
        this.verifications = verifications;
        this.customers = customers;
        this.cities = cities;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RegisterCustomerResponse register(RegisterCustomerRequest request) {
        PhoneVerification verification = verifications.find(request.verificationId())
                .orElseThrow(() -> new PhoneVerificationNotFoundException(request.verificationId()));
        if (customers.findByPhone(verification.phone()).isPresent()) {
            throw new PhoneAlreadyRegisteredException("a customer");
        }
        Language language = Accounts.language(request.language());
        Address address = Accounts.address(cities, request.addressLine(), request.latitude(), request.longitude());
        CustomerAccount account = CustomerAccount.register(verification, request.name(), address, language);

        verifications.save(verification);
        customers.save(account);
        publisher.publish(Accounts.registered(account, clock.instant()));
        return new RegisterCustomerResponse(account.id(), address.city().name());
    }
}
