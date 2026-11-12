package account.usecase.port.out;

/**
 * Makes the 6-digit codes sent by SMS. Infra uses a cryptographically secure random source, so a code
 * cannot be guessed from earlier ones.
 */
public interface VerificationCodes {

    String newCode();
}
