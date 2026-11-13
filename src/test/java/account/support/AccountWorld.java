package account.support;

import static account.support.AccountFixtures.CODE;
import static account.support.AccountFixtures.NOW;

import account.domain.CodeCheck;
import account.domain.PhoneNumber;
import account.usecase.GuestConfirmPhoneCode;
import account.usecase.GuestRegisterBusiness;
import account.usecase.GuestRegisterCustomer;
import account.usecase.GuestRequestPhoneCode;
import account.usecase.dto.ConfirmPhoneCodeRequest;
import account.usecase.dto.RegisterBusinessRequest;
import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.port.out.CityLocator;
import account.usecase.port.out.PhoneVerifier;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kernel.City;

/** The account context's ports, faked in memory, with shortcuts to reach the state a test needs. */
public final class AccountWorld {

    public final InMemoryPhoneVerificationRepository verifications = new InMemoryPhoneVerificationRepository();
    public final InMemoryCustomerAccountRepository customers = new InMemoryCustomerAccountRepository();
    public final InMemoryBusinessAccountRepository businesses = new InMemoryBusinessAccountRepository();
    public final InMemoryDataRequestRepository dataRequests = new InMemoryDataRequestRepository();
    public final RecordingAccountIEPublisher publisher = new RecordingAccountIEPublisher();
    public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    /** The phone numbers the SMS provider was asked to send a code to. */
    public final List<String> codesSentTo = new ArrayList<>();
    /** What the SMS provider answers for a code other than the right one. */
    public CodeCheck answerForOtherCodes = CodeCheck.WRONG;
    public final PhoneVerifier verifier = new PhoneVerifier() {
        @Override
        public void sendCode(PhoneNumber phone) {
            codesSentTo.add(phone.value());
        }

        @Override
        public CodeCheck checkCode(PhoneNumber phone, String code) {
            return CODE.equals(code) ? CodeCheck.PASSED : answerForOtherCodes;
        }
    };
    /** Where the geoservice finds every pin; empty when the pin is outside Homa's cities. */
    public Optional<City> cityOfPins = Optional.of(City.CYBERJAYA);
    public final CityLocator cities = pin -> cityOfPins;

    /** A phone number whose SMS code was requested and confirmed. Returns the verification's id. */
    public String verifiedPhone(String phone) {
        String verificationId = new GuestRequestPhoneCode(verifications, verifier)
                .request(new RequestPhoneCodeRequest(phone)).verificationId();
        new GuestConfirmPhoneCode(verifications, customers, businesses, verifier)
                .confirm(new ConfirmPhoneCodeRequest(verificationId, CODE));
        return verificationId;
    }

    public String customer(String phone) {
        return new GuestRegisterCustomer(verifications, customers, cities, publisher, clock)
                .register(new RegisterCustomerRequest(verifiedPhone(phone), "Aisyah binti Rahman",
                        "12 Jalan Teknokrat 5", 2.9213, 101.6559, "ENGLISH"))
                .customerId();
    }

    public String business(String phone) {
        return new GuestRegisterBusiness(verifications, businesses, publisher, clock)
                .register(new RegisterBusinessRequest(verifiedPhone(phone), "MALAY")).businessId();
    }

    public PhoneNumber phone(String typed) {
        return new PhoneNumber(typed);
    }
}
