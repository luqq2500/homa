package account.usecase;

import account.domain.BusinessAccount;
import account.domain.Language;
import account.domain.PhoneVerification;
import account.usecase.dto.RegisterBusinessRequest;
import account.usecase.dto.RegisterBusinessResponse;
import account.usecase.exception.PhoneAlreadyRegisteredException;
import account.usecase.exception.PhoneVerificationNotFoundException;
import account.usecase.port.in.GuestBusinessRegistrator;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.BusinessAccountRepository;
import account.usecase.port.out.PhoneVerificationRepository;
import ie.events.BusinessRegisteredIE;
import java.time.Clock;

public final class GuestRegisterBusiness implements GuestBusinessRegistrator {

    private final PhoneVerificationRepository verifications;
    private final BusinessAccountRepository businesses;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public GuestRegisterBusiness(PhoneVerificationRepository verifications, BusinessAccountRepository businesses,
                                 AccountIEPublisher publisher, Clock clock) {
        this.verifications = verifications;
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RegisterBusinessResponse register(RegisterBusinessRequest request) {
        PhoneVerification verification = verifications.find(request.verificationId())
                .orElseThrow(() -> new PhoneVerificationNotFoundException(request.verificationId()));
        if (businesses.findByPhone(verification.phone()).isPresent()) {
            throw new PhoneAlreadyRegisteredException("a business");
        }
        Language language = Accounts.language(request.language());
        BusinessAccount account = BusinessAccount.register(verification, language);

        verifications.save(verification);
        businesses.save(account);
        publisher.publish(new BusinessRegisteredIE(account.id(), language.name(), clock.instant()));
        return new RegisterBusinessResponse(account.id());
    }
}
