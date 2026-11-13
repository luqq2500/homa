package account.usecase;

import account.domain.BusinessAccount;
import account.usecase.dto.ChooseBusinessLanguageRequest;
import account.usecase.dto.ChooseBusinessLanguageResponse;
import account.usecase.port.in.BusinessLanguageChooser;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.BusinessAccountRepository;
import ie.events.BusinessLanguageChosenIE;
import java.time.Clock;

public final class BusinessChooseLanguage implements BusinessLanguageChooser {

    private final BusinessAccountRepository businesses;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public BusinessChooseLanguage(BusinessAccountRepository businesses, AccountIEPublisher publisher, Clock clock) {
        this.businesses = businesses;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChooseBusinessLanguageResponse choose(ChooseBusinessLanguageRequest request) {
        BusinessAccount account = Accounts.business(businesses, request.businessId());
        account.chooseLanguage(Accounts.language(request.language()));
        businesses.save(account);
        publisher.publish(new BusinessLanguageChosenIE(account.id(), account.language().name(), clock.instant()));
        return new ChooseBusinessLanguageResponse(account.id(), account.language().name());
    }
}
