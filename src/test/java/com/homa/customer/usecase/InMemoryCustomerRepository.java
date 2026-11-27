package com.homa.customer.usecase;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerId;
import com.homa.customer.domain.PersonId;
import com.homa.customer.domain.SignInId;
import com.homa.customer.domain.VerifiedIdentity;
import com.homa.customer.usecase.port.out.CustomerRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customer accounts in a map. It keeps the instances it was given, so each save is also counted: a test asserts
 * what was saved, not only what the shared instance now holds.
 */
final class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<CustomerId, Customer> customers = new LinkedHashMap<>();
    private final List<CustomerId> saves = new ArrayList<>();

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.ofNullable(customers.get(id));
    }

    @Override
    public Optional<CustomerId> findIdBySignIn(SignInId signInId) {
        return customers.values().stream()
                .filter(customer -> customer.signInId().equals(signInId))
                .map(Customer::id)
                .findFirst();
    }

    @Override
    public Optional<CustomerId> findIdByPersonId(PersonId personId) {
        return customers.values().stream()
                .filter(customer -> customer.verifiedIdentity()
                        .map(VerifiedIdentity::personId)
                        .filter(personId::equals)
                        .isPresent())
                .map(Customer::id)
                .findFirst();
    }

    @Override
    public void save(Customer customer) {
        customers.put(customer.id(), customer);
        saves.add(customer.id());
    }

    /** Puts a customer in place without counting it as a save by the code under test. */
    Customer given(Customer customer) {
        customers.put(customer.id(), customer);
        return customer;
    }

    Customer get(String customerId) {
        return customers.get(new CustomerId(customerId));
    }

    List<Customer> all() {
        return List.copyOf(customers.values());
    }

    /** The ids of the customers saved, in order, one entry per save. */
    List<CustomerId> saves() {
        return List.copyOf(saves);
    }
}
