package account.usecase;

import account.domain.DataRequest;
import account.usecase.dto.HandleDataRequestRequest;
import account.usecase.dto.HandleDataRequestResponse;
import account.usecase.exception.DataRequestNotFoundException;
import account.usecase.port.in.AdminDataRequestHandler;
import account.usecase.port.out.DataRequestRepository;
import java.time.Clock;
import java.time.Instant;

public final class AdminHandleDataRequest implements AdminDataRequestHandler {

    private final DataRequestRepository requests;
    private final Clock clock;

    public AdminHandleDataRequest(DataRequestRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    @Override
    public HandleDataRequestResponse handle(HandleDataRequestRequest request) {
        Instant now = clock.instant();
        DataRequest dataRequest = requests.find(request.requestId())
                .orElseThrow(() -> new DataRequestNotFoundException(request.requestId()));
        dataRequest.handle(request.adminId(), request.note(), now);
        requests.save(dataRequest);
        return new HandleDataRequestResponse(dataRequest.id(), now);
    }
}
