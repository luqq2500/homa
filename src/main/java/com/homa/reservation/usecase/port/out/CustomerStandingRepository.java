package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.replica.CustomerStanding;
import java.util.Optional;

/** Registered customers as Reservation sees them, from Customer's events (CUS-3, RES-1). */
public interface CustomerStandingRepository {

    Optional<CustomerStanding> findById(String customerId);

    void save(CustomerStanding customer);
}
