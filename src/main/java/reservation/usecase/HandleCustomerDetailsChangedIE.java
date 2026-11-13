package reservation.usecase;

import ie.events.CustomerDetailsChangedIE;
import reservation.domain.Address;
import reservation.domain.Customer;
import reservation.usecase.port.in.CustomerDetailsChangedIEHandler;
import reservation.usecase.port.out.CustomerRepository;

public final class HandleCustomerDetailsChangedIE implements CustomerDetailsChangedIEHandler {

    private final CustomerRepository customers;

    public HandleCustomerDetailsChangedIE(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void handle(CustomerDetailsChangedIE event) {
        customers.save(new Customer(event.customerId(), event.name(),
                new Address(event.addressLine(), event.pin(), event.city()), event.accessNotes()));
    }
}
