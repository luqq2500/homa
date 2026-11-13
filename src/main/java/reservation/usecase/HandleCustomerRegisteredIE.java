package reservation.usecase;

import ie.events.CustomerRegisteredIE;
import reservation.domain.Address;
import reservation.domain.Customer;
import reservation.usecase.port.in.CustomerRegisteredIEHandler;
import reservation.usecase.port.out.CustomerRepository;

public final class HandleCustomerRegisteredIE implements CustomerRegisteredIEHandler {

    private final CustomerRepository customers;

    public HandleCustomerRegisteredIE(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void handle(CustomerRegisteredIE event) {
        customers.save(new Customer(event.customerId(), event.name(),
                new Address(event.addressLine(), event.pin(), event.city()), event.accessNotes()));
    }
}
