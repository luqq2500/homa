package com.homa.customer.usecase;

import com.homa.customer.domain.ArrivalNote;
import com.homa.customer.domain.CityCode;
import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerName;
import com.homa.customer.domain.HomeAddress;
import com.homa.customer.domain.SignInId;
import com.homa.customer.domain.SignInStatus;
import com.homa.customer.domain.TermsAcceptance;
import com.homa.customer.usecase.dto.RegisterAccountRequest;
import com.homa.customer.usecase.dto.RegisterAccountResponse;
import com.homa.customer.usecase.port.in.CustomerRegistrator;
import com.homa.customer.usecase.port.out.CustomerIEPublisher;
import com.homa.customer.usecase.port.out.CustomerRepository;
import com.homa.customer.usecase.port.out.EventIds;
import com.homa.customer.usecase.port.out.TimeSource;
import com.homa.ie.events.CustomerRegisteredIE;
import java.time.Instant;

/**
 * CUS-1, CUS-2, CUS-7: registers the person who signed in. The sign-in is looked up first, because one
 * identity-provider account holds one customer account across every customer; the aggregate refuses one that is
 * taken. The domain's refusals reach the caller unchanged. Publishes {@link CustomerRegisteredIE}, from which
 * Reservation keeps the customer's standing and city.
 */
public class CustomerRegisterAccount implements CustomerRegistrator {

    private final CustomerRepository customers;
    private final CustomerIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public CustomerRegisterAccount(CustomerRepository customers, CustomerIEPublisher publisher, TimeSource time,
            EventIds eventIds) {
        this.customers = customers;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public RegisterAccountResponse register(RegisterAccountRequest request) {
        Instant now = time.now();
        SignInId signIn = new SignInId(request.issuer(), request.subject());
        SignInStatus signInStatus = customers.findIdBySignIn(signIn)
                .map(holder -> SignInStatus.taken())
                .orElseGet(SignInStatus::free);
        Customer customer = Customer.register(signIn, signInStatus,
                new CustomerName(request.name()),
                new HomeAddress(request.streetAddress(), new CityCode(request.city())),
                new ArrivalNote(request.arrivalNote()),
                TermsAcceptance.of(request.termsOfServiceAgreed(), now));
        customers.save(customer);
        String status = customer.statusAt(now).name();
        publisher.publish(new CustomerRegisteredIE(eventIds.next(), customer.id().value(),
                customer.homeAddress().city().value(), status, now));
        return new RegisterAccountResponse(customer.id().value(), status);
    }
}
