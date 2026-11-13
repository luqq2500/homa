package account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.InvalidPhoneNumberException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"0123456789", "012-345 6789", "+60123456789", "60123456789", "+60 12-345 6789"})
    void aMalaysianMobileNumberIsKeptInInternationalFormHoweverItIsTyped(String typed) {
        assertThat(new PhoneNumber(typed).value()).isEqualTo("+60123456789");
    }

    @Test
    void anElevenDigitMobileNumberIsAccepted() {
        assertThat(new PhoneNumber("01112345678").value()).isEqualTo("+601112345678");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "012345678", "011123456789", "0323456789", "+6512345678", "01234abcde", "6012345678"})
    void anythingElseIsNotAMalaysianMobileNumber(String typed) {
        assertThatThrownBy(() -> new PhoneNumber(typed)).isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    void theSameNumberTypedTwoWaysIsTheSameNumber() {
        assertThat(new PhoneNumber("0123456789")).isEqualTo(new PhoneNumber("+60 12 345 6789"));
    }
}
