package com.homa.conversation.usecase.port.out;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import java.util.Optional;

/**
 * The replica of who is registered, verified and suspended, kept from Customer's events (CHT-1, CUS-5). Infra
 * rebuilds each with {@link CustomerStanding#restore}.
 */
public interface CustomerStandingRepository {

    Optional<CustomerStanding> findById(CustomerId customerId);

    /** Adds the customer's standing or stores its changes. */
    void save(CustomerStanding standing);
}
