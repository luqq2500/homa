package account.usecase.port.out;

import ie.port.AccountIE;

public interface AccountIEPublisher {

    void publish(AccountIE event);
}
