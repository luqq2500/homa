package com.homa.reservation.usecase;

import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.replica.PersonPhoto;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.PersonPhotoRepository;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The replicas Reservation keeps of other contexts' facts, in memory. They are immutable values, kept as given. */
public final class InMemoryReplicas {

    private InMemoryReplicas() {
    }

    public static final class Providers implements BookableProviderRepository {
        private final Map<String, ProviderReplica> stored = new LinkedHashMap<>();

        @Override
        public Optional<ProviderReplica> findById(String providerId) {
            return Optional.ofNullable(stored.get(providerId));
        }

        @Override
        public void save(ProviderReplica provider) {
            stored.put(provider.providerId(), provider);
        }

        public int count() {
            return stored.size();
        }
    }

    public static final class Members implements RosterableMemberRepository {
        private final Map<String, RosterableMember> stored = new LinkedHashMap<>();

        @Override
        public Optional<RosterableMember> findById(String memberId) {
            return Optional.ofNullable(stored.get(memberId));
        }

        @Override
        public List<RosterableMember> findByProviderId(String providerId) {
            return stored.values().stream().filter(member -> member.providerId().equals(providerId)).toList();
        }

        @Override
        public List<RosterableMember> findByPersonId(String personId) {
            return stored.values().stream().filter(member -> member.personId().equals(personId)).toList();
        }

        @Override
        public void save(RosterableMember member) {
            stored.put(member.memberId(), member);
        }

        public int count() {
            return stored.size();
        }
    }

    public static final class Photos implements PersonPhotoRepository {
        private final Map<String, PersonPhoto> stored = new LinkedHashMap<>();

        @Override
        public Optional<PersonPhoto> findByPersonId(String personId) {
            return Optional.ofNullable(stored.get(personId));
        }

        @Override
        public void save(PersonPhoto photo) {
            stored.put(photo.personId(), photo);
        }
    }

    public static final class Customers implements CustomerStandingRepository {
        private final Map<String, CustomerStanding> stored = new LinkedHashMap<>();

        @Override
        public Optional<CustomerStanding> findById(String customerId) {
            return Optional.ofNullable(stored.get(customerId));
        }

        @Override
        public void save(CustomerStanding customer) {
            stored.put(customer.customerId(), customer);
        }

        /** As if Reservation had never heard of the customer. */
        public void forget(String customerId) {
            stored.remove(customerId);
        }

        public int count() {
            return stored.size();
        }
    }
}
