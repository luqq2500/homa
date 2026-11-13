package account.usecase.port.in;

import account.usecase.dto.CloseCustomerAccountRequest;
import account.usecase.dto.CloseCustomerAccountResponse;

/** A customer closes their account. */
public interface CustomerAccountCloser {

    CloseCustomerAccountResponse close(CloseCustomerAccountRequest request);
}
