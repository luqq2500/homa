package account.domain;

import static account.support.AccountFixtures.HOME;
import static account.support.AccountFixtures.NOW;
import static account.support.AccountFixtures.PHONE;
import static account.support.AccountFixtures.PIN;
import static account.support.AccountFixtures.customer;
import static account.support.AccountFixtures.verified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccessNotesTooLongException;
import account.domain.exception.AccountClosedException;
import account.domain.exception.AddressIncompleteException;
import account.domain.exception.NameMissingException;
import account.domain.exception.PhoneNotVerifiedException;
import kernel.City;
import org.junit.jupiter.api.Test;

class CustomerAccountTest {

    @Test
    void aCustomerRegistersWithTheirVerifiedPhoneNameAndAddress() {
        PhoneVerification verification = verified(PHONE);

        CustomerAccount account = CustomerAccount.register(verification, " Aisyah binti Rahman ", HOME, Language.MALAY);

        assertThat(account.id()).isNotBlank().isNotEqualTo(customer().id());
        assertThat(account.phone()).isEqualTo(PHONE);
        assertThat(account.name()).isEqualTo("Aisyah binti Rahman");
        assertThat(account.address()).isEqualTo(HOME);
        assertThat(account.accessNotes()).isEmpty();
        assertThat(account.language()).isEqualTo(Language.MALAY);
        assertThat(account.closedAt()).isEmpty();
        assertThat(verification.isUsed()).isTrue();
    }

    @Test
    void aCustomerCannotRegisterWithAPhoneNumberThatIsNotVerified() {
        PhoneVerification unconfirmed = PhoneVerification.start(PHONE);

        assertThatThrownBy(() -> CustomerAccount.register(unconfirmed, "Aisyah", HOME, Language.ENGLISH))
                .isInstanceOf(PhoneNotVerifiedException.class);
    }

    @Test
    void aCustomerGivesTheirNameAndAMissingNameDoesNotUseUpTheVerification() {
        PhoneVerification verification = verified(PHONE);

        assertThatThrownBy(() -> CustomerAccount.register(verification, " ", HOME, Language.ENGLISH))
                .isInstanceOf(NameMissingException.class);
        assertThatThrownBy(() -> CustomerAccount.register(verification, null, HOME, Language.ENGLISH))
                .isInstanceOf(NameMissingException.class);
        assertThat(verification.isUsed()).isFalse();
    }

    @Test
    void anAddressHasItsTextPinAndCity() {
        assertThat(new Address(" 12 Jalan Teknokrat 5 ", PIN, City.CYBERJAYA).line()).isEqualTo("12 Jalan Teknokrat 5");
        assertThatThrownBy(() -> new Address(" ", PIN, City.CYBERJAYA)).isInstanceOf(AddressIncompleteException.class);
        assertThatThrownBy(() -> new Address(null, PIN, City.CYBERJAYA)).isInstanceOf(AddressIncompleteException.class);
        assertThatThrownBy(() -> new Address("12 Jalan", null, City.CYBERJAYA)).isInstanceOf(AddressIncompleteException.class);
        assertThatThrownBy(() -> new Address("12 Jalan", PIN, null)).isInstanceOf(AddressIncompleteException.class);
    }

    @Test
    void aCustomerChangesTheirNameAndAddress() {
        CustomerAccount account = customer();
        Address newHome = new Address("3 Presint 8", PIN, City.PUTRAJAYA);

        account.changeName(" Aisyah Rahman ");
        account.changeAddress(newHome);

        assertThat(account.name()).isEqualTo("Aisyah Rahman");
        assertThat(account.address()).isEqualTo(newHome);
        assertThatThrownBy(() -> account.changeName("")).isInstanceOf(NameMissingException.class);
    }

    @Test
    void accessNotesAreUpToFiveHundredCharacters() {
        CustomerAccount account = customer();

        account.changeAccessNotes("a".repeat(500));
        assertThat(account.accessNotes()).hasSize(500);

        assertThatThrownBy(() -> account.changeAccessNotes("a".repeat(501))).isInstanceOf(AccessNotesTooLongException.class);
        assertThat(account.accessNotes()).hasSize(500);

        account.changeAccessNotes(null);
        assertThat(account.accessNotes()).isEmpty();
    }

    @Test
    void aCustomerChoosesTheirLanguage() {
        CustomerAccount account = customer();

        account.chooseLanguage(Language.MALAY);

        assertThat(account.language()).isEqualTo(Language.MALAY);
    }

    @Test
    void aClosedAccountCanNoLongerBeChanged() {
        CustomerAccount account = customer();

        account.close(NOW);

        assertThat(account.closedAt()).contains(NOW);
        assertThatThrownBy(() -> account.changeName("Other")).isInstanceOf(AccountClosedException.class);
        assertThatThrownBy(() -> account.changeAddress(HOME)).isInstanceOf(AccountClosedException.class);
        assertThatThrownBy(() -> account.changeAccessNotes("Gate B")).isInstanceOf(AccountClosedException.class);
        assertThatThrownBy(() -> account.chooseLanguage(Language.MALAY)).isInstanceOf(AccountClosedException.class);
        assertThatThrownBy(() -> account.close(NOW.plusSeconds(1))).isInstanceOf(AccountClosedException.class);
        assertThat(account.name()).isEqualTo("Aisyah binti Rahman");
        assertThat(account.accessNotes()).isEmpty();
        assertThat(account.language()).isEqualTo(Language.ENGLISH);
        assertThat(account.closedAt()).contains(NOW);
    }

    @Test
    void aStoredAccountIsCheckedLikeANewOne() {
        assertThatThrownBy(() -> new CustomerAccount("c-1", PHONE, "Aisyah", HOME, "a".repeat(501), Language.ENGLISH, null))
                .isInstanceOf(AccessNotesTooLongException.class);
        assertThat(new CustomerAccount("c-1", PHONE, "Aisyah", HOME, "Gate B", Language.ENGLISH, NOW).accessNotes())
                .isEqualTo("Gate B");
    }
}
