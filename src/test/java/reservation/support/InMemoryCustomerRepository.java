package reservation.support;

import reservation.domain.Customer;
import reservation.usecase.port.out.CustomerRepository;
import support.InMemoryStore;

public final class InMemoryCustomerRepository extends InMemoryStore<Customer> implements CustomerRepository {

    public InMemoryCustomerRepository() {
        super(Customer::customerId);
    }
}
