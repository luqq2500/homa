package account.usecase.port.out;

/** Sends the phone verification code by SMS. */
public interface SmsSender {

    void sendVerificationCode(String phone, String code);
}
