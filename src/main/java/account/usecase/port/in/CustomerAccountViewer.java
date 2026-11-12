package account.usecase.port.in;

import account.usecase.dto.CustomerViewAccountRequest;
import account.usecase.dto.CustomerViewAccountResponse;

/** A customer views their account. */
public interface CustomerAccountViewer {

    CustomerViewAccountResponse view(CustomerViewAccountRequest request);
}
