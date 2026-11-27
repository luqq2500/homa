package com.homa.customer.usecase.port.out;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerId;
import com.homa.customer.domain.PersonId;
import com.homa.customer.domain.SignInId;
import java.util.Optional;

/**
 * Where customer accounts are kept. The two lookups serve the rules that span every customer: one sign-in holds one
 * account (CUS-7) and one verified identity holds one account (CUS-10). The use cases pass their answers to the
 * aggregate as {@code SignInStatus} and {@code IdentityStatus}. Unique indexes on the sign-in (issuer, subject) and
 * on the verified personId back them up when two writes race.
 */
public interface CustomerRepository {

    Optional<Customer> findById(CustomerId id);

    /** The account the sign-in holds, whatever that account's state. Empty if none. */
    Optional<CustomerId> findIdBySignIn(SignInId signInId);

    /** The account whose verified identity is this person, whatever that account's state. Empty if none. */
    Optional<CustomerId> findIdByPersonId(PersonId personId);

    /** Adds a new customer or stores the changes to an existing one. */
    void save(Customer customer);
}
