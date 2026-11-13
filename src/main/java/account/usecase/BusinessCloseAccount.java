package account.usecase;

import account.domain.BusinessAccount;
import account.usecase.dto.CloseBusinessAccountRequest;
import account.usecase.dto.CloseBusinessAccountResponse;
import account.usecase.port.in.BusinessAccountCloser;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.BusinessAccountRepository;
import ie.events.AccountClosedIE;
import java.time.Clock;
import java.time.Instant;

public final class BusinessCloseAccount implements BusinessAccountCloser {

    private final BusinessAccountRepository businesses;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public BusinessCloseAccount(BusinessAccountRepository businesses, AccountIEPublisher publisher, Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public CloseBusinessAccountResponse close(CloseBusinessAccountRequest request) {
        Instant now = clock.instant();
        BusinessAccount account = Accounts.business(businesses, request.businessId());
        account.close(now);
        businesses.save(account);
        publisher.publish(new AccountClosedIE(account.id(), "BUSINESS", now));
        return new CloseBusinessAccountResponse(account.id(), now);
    }
}
