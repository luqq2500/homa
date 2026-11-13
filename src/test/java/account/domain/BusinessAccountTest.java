package account.domain;

import static account.support.AccountFixtures.NOW;
import static account.support.AccountFixtures.PHONE;
import static account.support.AccountFixtures.business;
import static account.support.AccountFixtures.verified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountClosedException;
import account.domain.exception.PhoneNotVerifiedException;
import org.junit.jupiter.api.Test;

class BusinessAccountTest {

    @Test
    void aBusinessRegistersWithItsVerifiedPhoneNumber() {
        PhoneVerification verification = verified(PHONE);

        BusinessAccount account = BusinessAccount.register(verification, Language.MALAY);

        assertThat(account.id()).isNotBlank().isNotEqualTo(business().id());
        assertThat(account.phone()).isEqualTo(PHONE);
        assertThat(account.language()).isEqualTo(Language.MALAY);
        assertThat(account.closedAt()).isEmpty();
        assertThat(verification.isUsed()).isTrue();
    }

    @Test
    void aBusinessCannotRegisterWithAPhoneNumberThatIsNotVerified() {
        assertThatThrownBy(() -> BusinessAccount.register(PhoneVerification.start(PHONE), Language.MALAY))
                .isInstanceOf(PhoneNotVerifiedException.class);
    }

    @Test
    void aBusinessChoosesItsLanguage() {
        BusinessAccount account = business();

        account.chooseLanguage(Language.ENGLISH);

        assertThat(account.language()).isEqualTo(Language.ENGLISH);
    }

    @Test
    void aClosedBusinessAccountCanNoLongerBeChanged() {
        BusinessAccount account = business();

        account.close(NOW);

        assertThat(account.closedAt()).contains(NOW);
        assertThatThrownBy(() -> account.chooseLanguage(Language.ENGLISH)).isInstanceOf(AccountClosedException.class);
        assertThatThrownBy(() -> account.close(NOW.plusSeconds(1))).isInstanceOf(AccountClosedException.class);
        assertThat(account.language()).isEqualTo(Language.MALAY);
        assertThat(account.closedAt()).contains(NOW);
    }
}
