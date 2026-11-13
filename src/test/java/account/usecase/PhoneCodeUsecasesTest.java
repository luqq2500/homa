package account.usecase;

import static account.support.AccountFixtures.CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.CodeCheck;
import account.domain.exception.InvalidPhoneNumberException;
import account.domain.exception.PhoneCodeNotAcceptedException;
import account.support.AccountWorld;
import account.usecase.dto.ConfirmPhoneCodeRequest;
import account.usecase.dto.ConfirmPhoneCodeResponse;
import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.dto.RequestPhoneCodeResponse;
import account.usecase.exception.PhoneVerificationNotFoundException;
import org.junit.jupiter.api.Test;

class PhoneCodeUsecasesTest {

    private final AccountWorld world = new AccountWorld();
    private final GuestRequestPhoneCode requester = new GuestRequestPhoneCode(world.verifications, world.verifier);
    private final GuestConfirmPhoneCode confirmer =
            new GuestConfirmPhoneCode(world.verifications, world.customers, world.businesses, world.verifier);

    @Test
    void theSmsProviderIsAskedToSendACodeToTheNumber() {
        RequestPhoneCodeResponse response = requester.request(new RequestPhoneCodeRequest("012-345 6789"));

        assertThat(world.codesSentTo).containsExactly("+60123456789");
        assertThat(world.verifications.find(response.verificationId())).isPresent();
    }

    @Test
    void noCodeIsSentToANumberThatIsNotAMalaysianMobile() {
        assertThatThrownBy(() -> requester.request(new RequestPhoneCodeRequest("0323456789")))
                .isInstanceOf(InvalidPhoneNumberException.class);
        assertThat(world.codesSentTo).isEmpty();
    }

    @Test
    void aCodeTheSmsProviderPassesConfirmsANumberThatHasNoAccountYet() {
        String verificationId = requester.request(new RequestPhoneCodeRequest("0123456789")).verificationId();

        ConfirmPhoneCodeResponse response = confirmer.confirm(new ConfirmPhoneCodeRequest(verificationId, CODE));

        assertThat(response).isEqualTo(new ConfirmPhoneCodeResponse("+60123456789", null, null));
        assertThat(world.verifications.find(verificationId).orElseThrow().isConfirmed()).isTrue();
        assertThat(world.verifications.timesSaved(verificationId)).isEqualTo(2);
    }

    @Test
    void confirmingTellsWhichAccountsTheNumberAlreadyHas() {
        String customerId = world.customer("0123456789");
        String businessId = world.business("0123456789");
        world.business("0198765432");
        String verificationId = requester.request(new RequestPhoneCodeRequest("0123456789")).verificationId();

        ConfirmPhoneCodeResponse response = confirmer.confirm(new ConfirmPhoneCodeRequest(verificationId, CODE));

        assertThat(response).isEqualTo(new ConfirmPhoneCodeResponse("+60123456789", customerId, businessId));
    }

    @Test
    void aCodeTheSmsProviderRefusesConfirmsNothing() {
        String verificationId = requester.request(new RequestPhoneCodeRequest("0123456789")).verificationId();

        assertThatThrownBy(() -> confirmer.confirm(new ConfirmPhoneCodeRequest(verificationId, "000000")))
                .isInstanceOf(PhoneCodeNotAcceptedException.class).hasMessageContaining("WRONG");
        world.answerForOtherCodes = CodeCheck.EXPIRED;
        assertThatThrownBy(() -> confirmer.confirm(new ConfirmPhoneCodeRequest(verificationId, "000000")))
                .isInstanceOf(PhoneCodeNotAcceptedException.class).hasMessageContaining("EXPIRED");

        assertThat(world.verifications.find(verificationId).orElseThrow().isConfirmed()).isFalse();
        assertThat(world.verifications.timesSaved(verificationId)).isEqualTo(1);
    }

    @Test
    void aVerificationThatWasNeverStartedCannotBeConfirmed() {
        assertThatThrownBy(() -> confirmer.confirm(new ConfirmPhoneCodeRequest("none", CODE)))
                .isInstanceOf(PhoneVerificationNotFoundException.class);
    }
}
