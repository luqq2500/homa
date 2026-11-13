package account.domain;

import static account.support.AccountFixtures.PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.PhoneCodeNotAcceptedException;
import account.domain.exception.PhoneNotVerifiedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PhoneVerificationTest {

    private final PhoneVerification verification = PhoneVerification.start(PHONE);

    @Test
    void aStartedVerificationWaitsToBeConfirmed() {
        assertThat(verification.id()).isNotBlank().isNotEqualTo(PhoneVerification.start(PHONE).id());
        assertThat(verification.phone()).isEqualTo(PHONE);
        assertThat(verification.isConfirmed()).isFalse();
        assertThat(verification.isUsed()).isFalse();
    }

    @Test
    void aCodeTheSmsProviderPassedConfirmsThePhoneNumber() {
        verification.confirm(CodeCheck.PASSED);

        assertThat(verification.isConfirmed()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CodeCheck.class, names = {"WRONG", "EXPIRED", "TOO_MANY_TRIES"})
    void aCodeTheSmsProviderDidNotPassConfirmsNothing(CodeCheck check) {
        assertThatThrownBy(() -> verification.confirm(check)).isInstanceOf(PhoneCodeNotAcceptedException.class)
                .hasMessageContaining(check.name());
        assertThat(verification.isConfirmed()).isFalse();
    }

    @Test
    void anUnconfirmedPhoneNumberCannotRegisterAnAccount() {
        assertThatThrownBy(verification::useToRegister).isInstanceOf(PhoneNotVerifiedException.class);
        assertThat(verification.isUsed()).isFalse();
    }

    @Test
    void aConfirmedVerificationRegistersOneAccountOnly() {
        verification.confirm(CodeCheck.PASSED);

        assertThat(verification.useToRegister()).isEqualTo(PHONE);
        assertThat(verification.isUsed()).isTrue();
        assertThatThrownBy(verification::useToRegister).isInstanceOf(PhoneNotVerifiedException.class);
    }

    @Test
    void aStoredVerificationKeepsItsState() {
        PhoneVerification stored = new PhoneVerification("v-1", PHONE, true, true);

        assertThat(stored.id()).isEqualTo("v-1");
        assertThat(stored.isConfirmed()).isTrue();
        assertThat(stored.isUsed()).isTrue();
    }
}
