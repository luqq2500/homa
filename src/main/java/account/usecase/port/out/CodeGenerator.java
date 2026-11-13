package account.usecase.port.out;

/** Makes the 6-digit codes sent by SMS; they must not be guessable. */
public interface CodeGenerator {

    String sixDigits();
}
