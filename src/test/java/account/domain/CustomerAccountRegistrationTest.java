package account.domain;

import static account.domain.AccountBuilders.PHONE;
import static account.domain.AccountBuilders.aConfirmedPhoneVerification;
import static account.domain.AccountBuilders.anAddressIn;
import static account.domain.AccountBuilders.anUnconfirmedPhoneVerification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccessNotesTooLongException;
import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.PhoneNotVerifiedException;
import org.junit.jupiter.api.Test;

/** Feature: Customer registration. */
class CustomerAccountRegistrationTest {

    @Test
    void aCustomerCanOnlyRegisterWithAConfirmedPhoneNumber() {
        assertThatThrownBy(() -> CustomerAccount.register(anUnconfirmedPhoneVerification(), "Hakim", anAddressIn("Cyberjaya")))
                .isInstanceOf(PhoneNotVerifiedException.class)
                .hasMessageContaining("+60123456789");
    }

    @Test
    void aRegisteredCustomerHasTheConfirmedPhoneTheirNameAndAddress() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), " Hakim ", anAddressIn("Cyberjaya"));

        assertThat(customer.phone()).isEqualTo(PHONE);
        assertThat(customer.name()).isEqualTo("Hakim");
        assertThat(customer.address()).isEqualTo(anAddressIn("Cyberjaya"));
        assertThat(customer.id()).isNotBlank();
    }

    @Test
    void aCustomerNeedsANameProvidersCanSee() {
        assertThatThrownBy(() -> CustomerAccount.register(aConfirmedPhoneVerification(), " ", anAddressIn("Cyberjaya")))
                .isInstanceOf(AccountDetailRequiredException.class);
    }

    @Test
    void aCustomerNeedsAnAddress() {
        assertThatThrownBy(() -> CustomerAccount.register(aConfirmedPhoneVerification(), "Hakim", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aCustomerCanMoveToANewAddress() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Hakim", anAddressIn("Cyberjaya"));

        customer.changeAddress(anAddressIn("Putrajaya"));

        assertThat(customer.address().city()).isEqualTo(City.of("Putrajaya"));
        assertThatThrownBy(() -> customer.changeAddress(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aCustomerChangesTheirName() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Siti", anAddressIn("Cyberjaya"));

        boolean changed = customer.rename(" Siti Aminah ");

        assertThat(changed).isTrue();
        assertThat(customer.name()).isEqualTo("Siti Aminah");
    }

    @Test
    void changingToTheSameNameChangesNothing() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Siti", anAddressIn("Cyberjaya"));

        assertThat(customer.rename(" Siti ")).isFalse();
        assertThat(customer.name()).isEqualTo("Siti");
    }

    @Test
    void aNewNameIsRequired() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Siti", anAddressIn("Cyberjaya"));

        assertThatThrownBy(() -> customer.rename(" "))
                .isInstanceOf(AccountDetailRequiredException.class)
                .hasMessage("A customer's name is required");
        assertThat(customer.name()).isEqualTo("Siti");
    }

    @Test
    void aCustomerKeepsAccessNotesOnTheirAccount() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Siti", anAddressIn("Cyberjaya"));

        assertThat(customer.accessNotes().isNone()).isTrue();
        assertThat(customer.changeAccessNotes(" Unit 12-3, register at the guardhouse ")).isTrue();

        assertThat(customer.accessNotes().text()).isEqualTo("Unit 12-3, register at the guardhouse");
        assertThat(customer.accessNotes().isNone()).isFalse();
        assertThat(customer.changeAccessNotes("Unit 12-3, register at the guardhouse")).isFalse();
    }

    @Test
    void blankAccessNotesClearThem() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Siti", anAddressIn("Cyberjaya"));
        customer.changeAccessNotes("Park at B2");

        assertThat(customer.changeAccessNotes(" ")).isTrue();
        assertThat(customer.accessNotes()).isEqualTo(AccessNotes.NONE);
        assertThat(customer.changeAccessNotes(null)).isFalse();
    }

    @Test
    void accessNotesAreAtMost500Characters() {
        CustomerAccount customer = CustomerAccount.register(aConfirmedPhoneVerification(), "Siti", anAddressIn("Cyberjaya"));

        assertThat(customer.changeAccessNotes("x".repeat(500))).isTrue();
        assertThatThrownBy(() -> customer.changeAccessNotes("x".repeat(501)))
                .isInstanceOf(AccessNotesTooLongException.class)
                .hasMessage("Access notes are at most 500 characters, were 501");
        assertThat(customer.accessNotes().text()).hasSize(500);
    }
}
