package com.homa.customer.usecase.ie;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerId;
import com.homa.customer.domain.VerificationRefusal;
import com.homa.customer.usecase.port.in.PersonVerificationRefusedIEHandler;
import com.homa.customer.usecase.port.out.CustomerRepository;
import com.homa.customer.usecase.port.out.IdentityRefusal;
import com.homa.customer.usecase.port.out.IdentityRefusals;
import com.homa.customer.usecase.port.out.ProcessedMessageStore;
import com.homa.customer.usecase.port.out.TimeSource;
import com.homa.ie.events.PersonVerificationRefusedIE;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * CUS-10: Verification refused the identity a customer verified with, and never stores a person it refused, so this
 * is how Customer learns why the customer can't reserve. The refusal is recorded on the customer account that
 * started the verification, so the customer is told: under 18, a status the country policy doesn't allow for
 * customers, or an identity that already holds another account. That last one is also kept through
 * {@link IdentityRefusals}, like the CUS-10 refusals Customer makes itself. A document not accepted or a check not
 * passed changes nothing: the customer verifies again. Verifications of owners and members are Provider's, so they
 * are ignored.
 *
 * <p>Idempotent (ADR 0002): a redelivered event does nothing. Publishes nothing.
 */
public class HandlePersonVerificationRefusedIE implements PersonVerificationRefusedIEHandler {

    /** Stable across renames of the class: changing it would make every old event look new to this handler. */
    public static final String HANDLER = "customer.PersonVerificationRefused";
    static final String CUSTOMER_ROLE = "CUSTOMER";

    /** The refusals that tell the customer why; in Malaysia only a local may be a customer (§14). */
    private static final Map<String, VerificationRefusal> BY_REASON = Map.of(
            "UNDER_MINIMUM_AGE", VerificationRefusal.UNDER_MINIMUM_AGE,
            "STATUS_NOT_ALLOWED_FOR_ROLE", VerificationRefusal.NOT_LOCAL,
            "IDENTITY_ALREADY_HELD", VerificationRefusal.IDENTITY_HELD_BY_ANOTHER_ACCOUNT);

    private final ProcessedMessageStore processedMessages;
    private final CustomerRepository customers;
    private final IdentityRefusals refusals;
    private final TimeSource time;

    public HandlePersonVerificationRefusedIE(ProcessedMessageStore processedMessages, CustomerRepository customers,
            IdentityRefusals refusals, TimeSource time) {
        this.processedMessages = processedMessages;
        this.customers = customers;
        this.refusals = refusals;
        this.time = time;
    }

    @Override
    public void handle(PersonVerificationRefusedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        if (!CUSTOMER_ROLE.equals(event.role())) {
            return;
        }
        Optional.ofNullable(BY_REASON.get(event.reason())).ifPresent(refusal -> accountOf(event).ifPresentOrElse(
                customer -> record(event, customer, refusal),
                () -> keep(event, IdentityRefusal.Reason.ACCOUNT_NOT_FOUND)));
    }

    private Optional<Customer> accountOf(PersonVerificationRefusedIE event) {
        return Optional.ofNullable(event.accountId())
                .filter(Predicate.not(String::isBlank))
                .map(CustomerId::new)
                .flatMap(customers::findById);
    }

    private void record(PersonVerificationRefusedIE event, Customer customer, VerificationRefusal refusal) {
        customer.recordVerificationRefused(refusal);
        customers.save(customer);
        if (refusal == VerificationRefusal.IDENTITY_HELD_BY_ANOTHER_ACCOUNT) {
            keep(event, IdentityRefusal.Reason.IDENTITY_HOLDS_ANOTHER_ACCOUNT);
        }
    }

    private void keep(PersonVerificationRefusedIE event, IdentityRefusal.Reason reason) {
        refusals.record(new IdentityRefusal(event.eventId(), event.accountId(), event.personId(), reason,
                time.now()));
    }
}
