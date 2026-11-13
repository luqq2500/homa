package account.usecase;

import account.domain.DataRequest;
import account.domain.DataRequestKind;
import account.usecase.dto.RequestDataRequest;
import account.usecase.dto.RequestDataResponse;
import account.usecase.exception.AccountNotFoundException;
import account.usecase.exception.UnknownDataRequestKindException;
import account.usecase.port.in.UserDataRequester;
import account.usecase.port.out.BusinessAccountRepository;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.DataRequestRepository;
import java.time.Clock;

public final class UserRequestData implements UserDataRequester {

    private final DataRequestRepository requests;
    private final CustomerAccountRepository customers;
    private final BusinessAccountRepository businesses;
    private final Clock clock;

    public UserRequestData(DataRequestRepository requests, CustomerAccountRepository customers,
                           BusinessAccountRepository businesses, Clock clock) {
        this.requests = requests;
        this.customers = customers;
        this.businesses = businesses;
        this.clock = clock;
    }

    @Override
    public RequestDataResponse request(RequestDataRequest request) {
        String accountId = request.accountId();
        if (customers.find(accountId).isEmpty() && businesses.find(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        DataRequest dataRequest = DataRequest.make(accountId, kind(request.kind()), clock.instant());
        requests.save(dataRequest);
        return new RequestDataResponse(dataRequest.id());
    }

    private static DataRequestKind kind(String name) {
        try {
            return DataRequestKind.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownDataRequestKindException(name);
        }
    }
}
