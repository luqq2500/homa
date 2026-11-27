package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.replica.ProviderReplica;
import java.util.Optional;

/** What Reservation has heard about each provider, from which its {@code BookableProvider} is assembled. */
public interface BookableProviderRepository {

    Optional<ProviderReplica> findById(String providerId);

    void save(ProviderReplica provider);
}
