package account.domain;

import static account.support.AccountFixtures.CODE;
import static account.support.AccountFixtures.NOW;
import static account.support.AccountFixtures.PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.CodeExpiredException;
import account.domain.exception.InvalidCodeException;
import account.domain.exception.PhoneNotVerifiedException;
import account.domain.exception.TooManyWrongCodesException;
import account.domain.exception.WrongCodeException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneVerificationTest {

    private final PhoneVerification verification = PhoneVerification.send(PHONE, CODE, NOW);

    @Test
    void aSentCodeWaitsToBeConfirmed() {
        assertThat(verification.id()).isNotBlank().isNotEqualTo(PhoneVerification.send(PHONE, CODE, NOW).id());
        assertThat(verification.phone()).isEqualTo(PHONE);
        assertThat(verification.code()).isEqualTo(CODE);
        assertThat(verification.isConfirmed()).isFalse();
        assertThat(verification.isUsed()).isFalse();
        assertThat(verification.wrongCodes()).isZero();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"12345", "1234567", "12345a", ""})
    void theCodeIsSixDigits(String code) {
        assertThatThrownBy(() -> PhoneVerification.send(PHONE, code, NOW)).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void theRightCodeConfirmsThePhoneNumber() {
        verification.confirm(CODE, NOW.plusSeconds(60));

        assertThat(verification.isConfirmed()).isTrue();
        assertThat(verification.wrongCodes()).isZero();
    }

    @Test
    void theCodeIsValidForFiveMinutesIncludingTheLastMoment() {
        assertThat(verification.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));

        verification.confirm(CODE, NOW.plus(Duration.ofMinutes(5)));

        assertThat(verification.isConfirmed()).isTrue();
    }

    @Test
    void anExpiredCodeNoLongerConfirms() {
        assertThatThrownBy(() -> verification.confirm(CODE, NOW.plus(Duration.ofMinutes(5)).plusMillis(1)))
                .isInstanceOf(CodeExpiredException.class);
        assertThat(verification.isConfirmed()).isFalse();
    }

    @Test
    void aWrongCodeIsCountedAndRefused() {
        assertThatThrownBy(() -> verification.confirm("000000", NOW)).isInstanceOf(WrongCodeException.class);

        assertThat(verification.wrongCodes()).isEqualTo(1);
        assertThat(verification.isConfirmed()).isFalse();
    }

    @Test
    void theRightCodeStillWorksAfterFourWrongCodes() {
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> verification.confirm("000000", NOW)).isInstanceOf(WrongCodeException.class);
        }

        verification.confirm(CODE, NOW);

        assertThat(verification.isConfirmed()).isTrue();
    }

    @Test
    void afterFiveWrongCodesANewCodeIsNeeded() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> verification.confirm("000000", NOW)).isInstanceOf(WrongCodeException.class);
        }

        assertThatThrownBy(() -> verification.confirm(CODE, NOW)).isInstanceOf(TooManyWrongCodesException.class);
        assertThat(verification.isConfirmed()).isFalse();
        assertThat(verification.wrongCodes()).isEqualTo(5);
    }

    @Test
    void anUnconfirmedPhoneNumberCannotRegisterAnAccount() {
        assertThatThrownBy(verification::useToRegister).isInstanceOf(PhoneNotVerifiedException.class);
        assertThat(verification.isUsed()).isFalse();
    }

    @Test
    void aConfirmedVerificationRegistersOneAccountOnly() {
        verification.confirm(CODE, NOW);

        assertThat(verification.useToRegister()).isEqualTo(PHONE);
        assertThat(verification.isUsed()).isTrue();
        assertThatThrownBy(verification::useToRegister).isInstanceOf(PhoneNotVerifiedException.class);
    }

    @Test
    void aStoredVerificationKeepsItsState() {
        PhoneVerification stored = new PhoneVerification("v-1", PHONE, CODE, NOW, 5, true, true);

        assertThat(stored.id()).isEqualTo("v-1");
        assertThat(stored.wrongCodes()).isEqualTo(5);
        assertThat(stored.isConfirmed()).isTrue();
        assertThat(stored.isUsed()).isTrue();
        assertThatThrownBy(() -> stored.confirm(CODE, NOW)).isInstanceOf(TooManyWrongCodesException.class);
    }
}
