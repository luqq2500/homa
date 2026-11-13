package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.Customer;

public interface CustomerRepository {

    void save(Customer customer);

    Optional<Customer> find(String id);

    List<Customer> all();
}
