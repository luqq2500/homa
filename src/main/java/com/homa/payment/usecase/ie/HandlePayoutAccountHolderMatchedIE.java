package com.homa.payment.usecase.ie;

import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.exception.PayoutAccountNotFoundException;
import com.homa.payment.usecase.port.in.PayoutAccountHolderMatchedIEHandler;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.PayoutAccountRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * Verification's holder check came back: the provider's payout account is verified or not (PRV-2), and
 * {@link PayoutAccountVerifiedIE} tells Provider, which needs a verified account for bookability. A result for an
 * account the provider has since replaced, or a second result, changes nothing and publishes nothing.
 */
public class HandlePayoutAccountHolderMatchedIE implements PayoutAccountHolderMatchedIEHandler {

    static final String HANDLER = "payment.PayoutAccountHolderMatched";

    private final PayoutAccountRepository accounts;
    private final PaymentIEPublisher publisher;
    private final ProcessedMessageStore processedMessages;
    private final TimeSource time;
    private final EventIds eventIds;

    public HandlePayoutAccountHolderMatchedIE(PayoutAccountRepository accounts, PaymentIEPublisher publisher,
            ProcessedMessageStore processedMessages, TimeSource time, EventIds eventIds) {
        this.accounts = accounts;
        this.publisher = publisher;
        this.processedMessages = processedMessages;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public void handle(PayoutAccountHolderMatchedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        PayoutAccount account = accounts.findByProviderId(new ProviderId(event.providerId()))
                .orElseThrow(() -> new PayoutAccountNotFoundException(event.providerId()));
        if (!account.recordHolderCheck(new PayoutAccountId(event.payoutAccountId()), event.matched(),
                event.occurredAt())) {
            return;
        }
        accounts.save(account);
        publisher.publish(new PayoutAccountVerifiedIE(eventIds.next(), account.providerId().value(),
                account.accountId().value(), account.holder().name(), account.canReceivePayouts(), time.now()));
    }
}
