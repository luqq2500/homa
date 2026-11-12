package account.usecase.port.out;

import ie.port.AccountIE;

/** Publishes the account context's integration events to the other contexts. */
public interface AccountIEPublisher {

    void publish(AccountIE event);
}
