package account.usecase.port.in;

import account.usecase.dto.ChangeAddressRequest;
import account.usecase.dto.ChangeAddressResponse;

/** A customer changes their address. */
public interface CustomerAddressChanger {

    ChangeAddressResponse change(ChangeAddressRequest request);
}
