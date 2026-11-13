package account.usecase.port.out;

import account.domain.CodeCheck;
import account.domain.PhoneNumber;

/**
 * The SMS provider. It sends a verification code to a phone number and checks the code a person
 * types, by its own rules for the code's length, how long it is valid and how many tries are allowed.
 */
public interface PhoneVerifier {

    void sendCode(PhoneNumber phone);

    CodeCheck checkCode(PhoneNumber phone, String code);
}
