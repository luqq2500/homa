package com.homa.customer.usecase;

import com.homa.customer.domain.ArrivalNote;
import com.homa.customer.domain.CityCode;
import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerId;
import com.homa.customer.domain.CustomerName;
import com.homa.customer.domain.HomeAddress;
import com.homa.customer.usecase.dto.UpdateProfileRequest;
import com.homa.customer.usecase.dto.UpdateProfileResponse;
import com.homa.customer.usecase.exception.CustomerNotFoundException;
import com.homa.customer.usecase.exception.NotAllowedException;
import com.homa.customer.usecase.port.in.CustomerProfileUpdater;
import com.homa.customer.usecase.port.out.CustomerIEPublisher;
import com.homa.customer.usecase.port.out.CustomerRepository;
import com.homa.customer.usecase.port.out.EventIds;
import com.homa.customer.usecase.port.out.TimeSource;
import com.homa.ie.events.CustomerAddressChangedIE;
import java.time.Instant;
import java.util.Objects;

/**
 * CUS-1: the signed-in customer saves their profile. Only their own profile can change. Every detail is read before
 * any is applied, so a profile with one detail missing changes nothing. Publishes {@link CustomerAddressChangedIE}
 * when the customer moved or changed their arrival note, so Reservation copies the new city (RES-1) and new
 * reservations keep the new address and note (RES-8); a new name concerns only Customer.
 */
public class CustomerUpdateProfile implements CustomerProfileUpdater {

    private final CustomerRepository customers;
    private final CustomerIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public CustomerUpdateProfile(CustomerRepository customers, CustomerIEPublisher publisher, TimeSource time,
            EventIds eventIds) {
        this.customers = customers;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public UpdateProfileResponse update(UpdateProfileRequest request) {
        if (!Objects.equals(request.signedInCustomerId(), request.customerId())) {
            throw new NotAllowedException("A customer can change only their own profile");
        }
        CustomerId id = new CustomerId(request.customerId());
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id.value()));
        CustomerName name = new CustomerName(request.name());
        HomeAddress homeAddress = new HomeAddress(request.streetAddress(), new CityCode(request.city()));
        ArrivalNote arrivalNote = new ArrivalNote(request.arrivalNote());

        customer.changeName(name);
        boolean moved = customer.changeHomeAddress(homeAddress);
        boolean noteChanged = customer.changeArrivalNote(arrivalNote);
        customers.save(customer);
        if (moved || noteChanged) {
            announceAddress(customer);
        }
        return new UpdateProfileResponse(id.value(), name.value(), homeAddress.streetAddress(),
                homeAddress.city().value(), arrivalNote.value());
    }

    private void announceAddress(Customer customer) {
        Instant now = time.now();
        publisher.publish(new CustomerAddressChangedIE(eventIds.next(), customer.id().value(),
                customer.homeAddress().city().value(), customer.homeAddress().streetAddress(),
                customer.arrivalNote().value(), customer.statusAt(now).name(), now));
    }
}
