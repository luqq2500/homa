package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.replica.RosterableMember;
import java.util.List;
import java.util.Optional;

/**
 * Providers' team members as Reservation sees them. Each knows the person they are, as Verification knows them, so a
 * verified profile photo reaches every member that person is (IDN-4) and a rostered member acts as that person
 * (PRV-11).
 */
public interface RosterableMemberRepository {

    Optional<RosterableMember> findById(String memberId);

    List<RosterableMember> findByProviderId(String providerId);

    List<RosterableMember> findByPersonId(String personId);

    void save(RosterableMember member);
}
