package account.domain;

import static account.domain.AccountBuilders.PHONE;
import static account.domain.AccountBuilders.aConfirmedPhoneVerification;
import static account.domain.AccountBuilders.anAddressIn;
import static account.domain.AccountBuilders.anUnconfirmedPhoneVerification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
