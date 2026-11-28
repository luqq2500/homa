package com.homa.provider.usecase.ie;

import com.homa.ie.events.PersonVerifiedIE;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.usecase.port.in.PersonVerifiedIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import com.homa.provider.usecase.port.out.VerifiedPersonReplica;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Keeps what Verification last decided about a person who verified as an owner or a member, with their verified
 * name, so that opening a provider (PRV-1) and adding a member (HLP-1 to HLP-3) can judge them later. A verification
 * started for a customer account is Customer's (CUS-3, CUS-10) and carries no re-verification cycle, so Provider
 * ignores it. Events about one person arrive in order, so the latest replaces what was kept.
 *
 * <p>A new pass also tells the person's team member when the verification falls due next ({@code nextDueAt}), so that
 * a deactivated member who re-verified can be reactivated (HLP-7). Provider publishes nothing: the member's state
 * doesn't change.
 *
 * <p>Left for later slices: re-verification reactivating a suspended owner or member (MON-3), and a renewed permit
 * updating a member's {@code rosterableUntil}.
 */
public final class HandlePersonVerifiedIE implements PersonVerifiedIEHandler {

    static final String HANDLER = "provider.PersonVerified";
    static final String CUSTOMER = "CUSTOMER";
    static final String LOCAL = "LOCAL";

    private final ProcessedMessageStore processedMessages;
    private final VerifiedPersonReplica verifiedPeople;
    private final TeamMemberRepository members;

    public HandlePersonVerifiedIE(ProcessedMessageStore processedMessages, VerifiedPersonReplica verifiedPeople,
            TeamMemberRepository members) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.verifiedPeople = Objects.requireNonNull(verifiedPeople, "verifiedPeople");
        this.members = Objects.requireNonNull(members, "members");
    }

    @Override
    public void handle(PersonVerifiedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER) || CUSTOMER.equals(event.role())) {
            return;
        }
        VerifiedPersonFacts facts = factsOf(event);
        verifiedPeople.save(new VerifiedPerson(facts, event.name()));
        for (TeamMember member : members.findByPersonId(facts.personId())) {
            member.recordVerification(facts.verifiedUntil());
            members.save(member);
        }
    }

    /** LOCAL or FOREIGN, as Verification decided (§14); a foreign person always has a permit expiry. */
    private static VerifiedPersonFacts factsOf(PersonVerifiedIE event) {
        PersonId personId = new PersonId(event.personId());
        return LOCAL.equals(event.status())
                ? VerifiedPersonFacts.local(personId, event.ageCheckPassed(), event.nextDueAt())
                : VerifiedPersonFacts.foreign(personId, event.ageCheckPassed(), LocalDate.parse(event.permitExpiry()),
                        event.nextDueAt());
    }
}
