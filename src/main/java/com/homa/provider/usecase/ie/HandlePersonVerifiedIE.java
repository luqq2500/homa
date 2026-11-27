package com.homa.provider.usecase.ie;

import com.homa.ie.events.PersonVerifiedIE;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.usecase.port.in.PersonVerifiedIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
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
 * <p>Left for later slices: re-verification reactivating a suspended owner or member (MON-3, HLP-7), and a renewed
 * permit updating a member's {@code rosterableUntil}.
 */
public final class HandlePersonVerifiedIE implements PersonVerifiedIEHandler {

    static final String HANDLER = "provider.PersonVerified";
    static final String CUSTOMER = "CUSTOMER";
    static final String LOCAL = "LOCAL";

    private final ProcessedMessageStore processedMessages;
    private final VerifiedPersonReplica verifiedPeople;

    public HandlePersonVerifiedIE(ProcessedMessageStore processedMessages, VerifiedPersonReplica verifiedPeople) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.verifiedPeople = Objects.requireNonNull(verifiedPeople, "verifiedPeople");
    }

    @Override
    public void handle(PersonVerifiedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER) || CUSTOMER.equals(event.role())) {
            return;
        }
        verifiedPeople.save(new VerifiedPerson(factsOf(event), event.name()));
    }

    /** LOCAL or FOREIGN, as Verification decided (§14); a foreign person always has a permit expiry. */
    private static VerifiedPersonFacts factsOf(PersonVerifiedIE event) {
        PersonId personId = new PersonId(event.personId());
        return LOCAL.equals(event.status())
                ? VerifiedPersonFacts.local(personId, event.ageCheckPassed())
                : VerifiedPersonFacts.foreign(personId, event.ageCheckPassed(), LocalDate.parse(event.permitExpiry()));
    }
}
