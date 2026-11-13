package account.usecase.port.in;

import account.usecase.dto.ChangeAddressRequest;
import account.usecase.dto.ChangeAddressResponse;

/** A customer changes their address; it is used for new searches and requests only. */
public interface CustomerAddressChanger {

    ChangeAddressResponse change(ChangeAddressRequest request);
}
