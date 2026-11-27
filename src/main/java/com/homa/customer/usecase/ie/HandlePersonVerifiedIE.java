package com.homa.customer.usecase.ie;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerId;
import com.homa.customer.domain.IdentityStatus;
import com.homa.customer.domain.PersonId;
import com.homa.customer.domain.VerifiedIdentity;
import com.homa.customer.domain.exception.AccountHoldsAnotherIdentityException;
import com.homa.customer.domain.exception.IdentityAlreadyHoldsAccountException;
import com.homa.customer.usecase.port.in.PersonVerifiedIEHandler;
import com.homa.customer.usecase.port.out.CustomerIEPublisher;
import com.homa.customer.usecase.port.out.CustomerRepository;
import com.homa.customer.usecase.port.out.EventIds;
import com.homa.customer.usecase.port.out.IdentityRefusal;
import com.homa.customer.usecase.port.out.IdentityRefusals;
import com.homa.customer.usecase.port.out.ProcessedMessageStore;
import com.homa.customer.usecase.port.out.TimeSource;
import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.ie.events.PersonVerifiedIE;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * CUS-3, CUS-10: records what Verification found on the customer account that started the verification, and
 * announces with {@link CustomerVerifiedIE} the verification that first lets the customer reserve. Verifications of
 * owners and members are Provider's, so they are ignored. The personId is looked up first, because one verified
 * identity holds one customer account across every customer; the aggregate refuses one another account holds.
 *
 * <p>Idempotent (ADR 0002): a redelivered event does nothing. A refused verification is recorded through
 * {@link IdentityRefusals} and the event counts as handled, since the relay would only be refused again.
 */
public class HandlePersonVerifiedIE implements PersonVerifiedIEHandler {

    /** Stable across renames of the class: changing it would make every old event look new to this handler. */
    static final String HANDLER = "customer.PersonVerified";
    static final String CUSTOMER_ROLE = "CUSTOMER";
    static final String LOCAL = "LOCAL";

    private final ProcessedMessageStore processedMessages;
    private final CustomerRepository customers;
    private final IdentityRefusals refusals;
    private final CustomerIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public HandlePersonVerifiedIE(ProcessedMessageStore processedMessages, CustomerRepository customers,
            IdentityRefusals refusals, CustomerIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.processedMessages = processedMessages;
        this.customers = customers;
        this.refusals = refusals;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public void handle(PersonVerifiedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        if (!CUSTOMER_ROLE.equals(event.role())) {
            return;
        }
        accountOf(event).ifPresentOrElse(
                customer -> record(event, customer),
                () -> refuse(event, IdentityRefusal.Reason.ACCOUNT_NOT_FOUND));
    }

    private Optional<Customer> accountOf(PersonVerifiedIE event) {
        return Optional.ofNullable(event.accountId())
                .filter(Predicate.not(String::isBlank))
                .map(CustomerId::new)
                .flatMap(customers::findById);
    }

    private void record(PersonVerifiedIE event, Customer customer) {
        PersonId personId = new PersonId(event.personId());
        IdentityStatus identityStatus = customers.findIdByPersonId(personId)
                .map(IdentityStatus::heldBy)
                .orElseGet(IdentityStatus::free);
        VerifiedIdentity identity = new VerifiedIdentity(personId, LOCAL.equals(event.status()),
                event.ageCheckPassed());
        boolean nowVerified;
        try {
            nowVerified = customer.recordIdentityVerified(identity, identityStatus);
        } catch (IdentityAlreadyHoldsAccountException refused) {
            refuse(event, IdentityRefusal.Reason.IDENTITY_HOLDS_ANOTHER_ACCOUNT);
            return;
        } catch (AccountHoldsAnotherIdentityException refused) {
            refuse(event, IdentityRefusal.Reason.ACCOUNT_HOLDS_ANOTHER_IDENTITY);
            return;
        }
        customers.save(customer);
        if (nowVerified) {
            publisher.publish(new CustomerVerifiedIE(eventIds.next(), customer.id().value(), personId.value(),
                    time.now()));
        }
    }

    private void refuse(PersonVerifiedIE event, IdentityRefusal.Reason reason) {
        refusals.record(new IdentityRefusal(event.eventId(), event.accountId(), event.personId(), reason,
                time.now()));
    }
}
