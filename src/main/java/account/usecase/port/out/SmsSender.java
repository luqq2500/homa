package account.usecase.port.out;

import account.domain.PhoneNumber;

/** The SMS provider that delivers phone verification codes. */
public interface SmsSender {

    void sendCode(PhoneNumber phone, String code);
}
