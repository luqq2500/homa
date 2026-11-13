package account.usecase;

import static account.support.AccountFixtures.CODE;
import static account.support.AccountFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.InvalidPhoneNumberException;
import account.domain.exception.WrongCodeException;
import account.support.AccountWorld;
import account.usecase.dto.ConfirmPhoneCodeRequest;
import account.usecase.dto.ConfirmPhoneCodeResponse;
import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.dto.RequestPhoneCodeResponse;
import account.usecase.exception.PhoneVerificationNotFoundException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PhoneCodeUsecasesTest {

    private final AccountWorld world = new AccountWorld();
    private final GuestRequestPhoneCode requester =
            new GuestRequestPhoneCode(world.verifications, world.codes, world.sms, world.clock);
    private final GuestConfirmPhoneCode confirmer =
            new GuestConfirmPhoneCode(world.verifications, world.customers, world.businesses, world.clock);

    @Test
    void theCodeIsSentBySmsAndIsValidForFiveMinutes() {
        RequestPhoneCodeResponse response = requester.request(new RequestPhoneCodeRequest("012-345 6789"));

        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(world.sentSms).containsExactly("+60123456789 " + CODE);
        assertThat(world.verifications.find(response.verificationId())).isPresent();
    }

    @Test
    void noCodeIsSentToANumberThatIsNotAMalaysianMobile() {
        assertThatThrownBy(() -> requester.request(new RequestPhoneCodeRequest("0323456789")))
                .isInstanceOf(InvalidPhoneNumberException.class);
        assertThat(world.sentSms).isEmpty();
    }

    @Test
    void theRightCodeConfirmsANumberThatHasNoAccountYet() {
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
    void aWrongCodeIsCountedEvenThoughItIsRefused() {
        String verificationId = requester.request(new RequestPhoneCodeRequest("0123456789")).verificationId();

        assertThatThrownBy(() -> confirmer.confirm(new ConfirmPhoneCodeRequest(verificationId, "000000")))
                .isInstanceOf(WrongCodeException.class);

        assertThat(world.verifications.find(verificationId).orElseThrow().wrongCodes()).isEqualTo(1);
        assertThat(world.verifications.timesSaved(verificationId)).isEqualTo(2);
    }

    @Test
    void aCodeThatWasNeverSentCannotBeConfirmed() {
        assertThatThrownBy(() -> confirmer.confirm(new ConfirmPhoneCodeRequest("none", CODE)))
                .isInstanceOf(PhoneVerificationNotFoundException.class);
    }
}
