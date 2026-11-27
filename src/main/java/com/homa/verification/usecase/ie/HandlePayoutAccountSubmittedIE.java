package com.homa.verification.usecase.ie;

import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.verification.domain.PersonId;
import com.homa.verification.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.verification.usecase.port.in.PayoutAccountSubmittedIEHandler;
import com.homa.verification.usecase.port.out.AccountHolderCheck;
import com.homa.verification.usecase.port.out.EventIds;
import com.homa.verification.usecase.port.out.ProcessedMessageStore;
import com.homa.verification.usecase.port.out.ProviderOwners;
import com.homa.verification.usecase.port.out.TimeSource;
import com.homa.verification.usecase.port.out.VerificationIEPublisher;
import com.homa.verification.usecase.port.out.VerifiedPersonRepository;

/**
 * A provider submitted or changed its payout account (PRV-2, PRV-10). Verification alone holds identity numbers,
 * so it asks the payment provider's account-holder check whether the owner's verified identity holds the account,
 * and publishes only "matched" or "not matched" (IDN-3, §11). Every submission is checked afresh.
 *
 * <p>The owner comes from {@code ProviderOpenedIE}. Payment takes a payout account only from a provider whose owner
 * it knows, so an owner Verification doesn't know yet is on their way: the handler throws and the relay retries it.
 * Not matched, without asking, when the owner isn't verified: there is no verified identity to check against. A
 * BUSINESS holder is not matched in slice 1: the business's registered name is held by {@code BusinessVerification}
 * (PRV-7, slice 6), which doesn't exist yet. A failing check throws, so the relay retries it (docs/decisions/0002).
 */
public class HandlePayoutAccountSubmittedIE implements PayoutAccountSubmittedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "verification.PayoutAccountSubmitted";

    private static final String OWNER = "OWNER";

    private final ProcessedMessageStore processedMessages;
    private final ProviderOwners providerOwners;
    private final VerifiedPersonRepository persons;
    private final AccountHolderCheck holderCheck;
    private final VerificationIEPublisher publisher;
    private final EventIds eventIds;
    private final TimeSource time;

    public HandlePayoutAccountSubmittedIE(ProcessedMessageStore processedMessages, ProviderOwners providerOwners,
            VerifiedPersonRepository persons, AccountHolderCheck holderCheck, VerificationIEPublisher publisher,
            EventIds eventIds, TimeSource time) {
        this.processedMessages = processedMessages;
        this.providerOwners = providerOwners;
        this.persons = persons;
        this.holderCheck = holderCheck;
        this.publisher = publisher;
        this.eventIds = eventIds;
        this.time = time;
    }

    @Override
    public void handle(PayoutAccountSubmittedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        boolean matched = OWNER.equals(event.holder()) && heldByTheOwner(event);
        publisher.publish(new PayoutAccountHolderMatchedIE(eventIds.next(), event.payoutAccountId(),
                event.providerId(), event.holder(), matched, time.now()));
    }

    private boolean heldByTheOwner(PayoutAccountSubmittedIE event) {
        PersonId ownerId = providerOwners.ownerOf(event.providerId())
                .orElseThrow(() -> new ProviderOwnerNotYetKnownException(event.providerId()));
        return persons.findById(ownerId)
                .map(owner -> holderCheck.isHeldBy(event.payoutAccountId(), owner.identityNumber(), owner.name()))
                .orElse(false);
    }
}
