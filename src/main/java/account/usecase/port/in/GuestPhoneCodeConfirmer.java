package account.usecase.port.in;

import account.usecase.dto.ConfirmPhoneCodeRequest;
import account.usecase.dto.ConfirmPhoneCodeResponse;

/** Someone enters the SMS code; they learn which accounts the phone number already has. */
public interface GuestPhoneCodeConfirmer {

    ConfirmPhoneCodeResponse confirm(ConfirmPhoneCodeRequest request);
}
