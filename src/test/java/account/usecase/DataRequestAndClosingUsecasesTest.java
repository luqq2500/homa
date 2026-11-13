package account.usecase;

import static account.support.AccountFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.DataRequest;
import account.domain.DataRequestKind;
import account.domain.exception.AccountClosedException;
import account.domain.exception.DataRequestAlreadyHandledException;
import account.support.AccountWorld;
import account.usecase.dto.CloseBusinessAccountRequest;
import account.usecase.dto.CloseBusinessAccountResponse;
import account.usecase.dto.CloseCustomerAccountRequest;
import account.usecase.dto.CloseCustomerAccountResponse;
import account.usecase.dto.HandleDataRequestRequest;
import account.usecase.dto.HandleDataRequestResponse;
import account.usecase.dto.RequestDataRequest;
import account.usecase.exception.AccountNotFoundException;
import account.usecase.exception.DataRequestNotFoundException;
import account.usecase.exception.UnknownDataRequestKindException;
import ie.events.AccountClosedIE;
import org.junit.jupiter.api.Test;

class DataRequestAndClosingUsecasesTest {

    private final AccountWorld world = new AccountWorld();
    private final String customerId = world.customer("0123456789");
    private final String businessId = world.business("0198765432");
    private final UserRequestData requester =
            new UserRequestData(world.dataRequests, world.customers, world.businesses, world.clock);
    private final AdminHandleDataRequest handler = new AdminHandleDataRequest(world.dataRequests, world.clock);

    @Test
    void aCustomerOrABusinessCanAskForTheirData() {
        String fromCustomer = requester.request(new RequestDataRequest(customerId, "COPY")).requestId();
        String fromBusiness = requester.request(new RequestDataRequest(businessId, "DELETE")).requestId();

        DataRequest saved = world.dataRequests.find(fromCustomer).orElseThrow();
        assertThat(saved.accountId()).isEqualTo(customerId);
        assertThat(saved.kind()).isEqualTo(DataRequestKind.COPY);
        assertThat(saved.requestedAt()).isEqualTo(NOW);
        assertThat(world.dataRequests.find(fromBusiness).orElseThrow().kind()).isEqualTo(DataRequestKind.DELETE);
    }

    @Test
    void aDataRequestNeedsAnAccountAndAKnownKind() {
        assertThatThrownBy(() -> requester.request(new RequestDataRequest("none", "COPY")))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> requester.request(new RequestDataRequest(customerId, "SELL")))
                .isInstanceOf(UnknownDataRequestKindException.class);
        assertThatThrownBy(() -> requester.request(new RequestDataRequest(customerId, null)))
                .isInstanceOf(UnknownDataRequestKindException.class);
        assertThat(world.dataRequests.all()).isEmpty();
    }

    @Test
    void anAdminHandlesADataRequestOnceAndItIsRecorded() {
        String requestId = requester.request(new RequestDataRequest(customerId, "SEE")).requestId();

        HandleDataRequestResponse response = handler.handle(new HandleDataRequestRequest("admin-1", requestId, "Sent"));

        assertThat(response).isEqualTo(new HandleDataRequestResponse(requestId, NOW));
        assertThat(world.dataRequests.find(requestId).orElseThrow().handling())
                .contains(new DataRequest.Handling("admin-1", "Sent", NOW));
        assertThat(world.dataRequests.timesSaved(requestId)).isEqualTo(2);
        assertThatThrownBy(() -> handler.handle(new HandleDataRequestRequest("admin-2", requestId, "Again")))
                .isInstanceOf(DataRequestAlreadyHandledException.class);
        assertThatThrownBy(() -> handler.handle(new HandleDataRequestRequest("admin-1", "none", "Sent")))
                .isInstanceOf(DataRequestNotFoundException.class);
    }

    @Test
    void aCustomerClosesTheirAccount() {
        CustomerCloseAccount usecase = new CustomerCloseAccount(world.customers, world.publisher, world.clock);

        CloseCustomerAccountResponse response = usecase.close(new CloseCustomerAccountRequest(customerId));

        assertThat(response).isEqualTo(new CloseCustomerAccountResponse(customerId, NOW));
        assertThat(world.customers.find(customerId).orElseThrow().closedAt()).contains(NOW);
        assertThat(world.customers.timesSaved(customerId)).isEqualTo(2);
        assertThat(world.publisher.published(AccountClosedIE.class))
                .containsExactly(new AccountClosedIE(customerId, "CUSTOMER", NOW));
        assertThatThrownBy(() -> usecase.close(new CloseCustomerAccountRequest(customerId)))
                .isInstanceOf(AccountClosedException.class);
    }

    @Test
    void aBusinessClosesItsAccount() {
        BusinessCloseAccount usecase = new BusinessCloseAccount(world.businesses, world.publisher, world.clock);

        CloseBusinessAccountResponse response = usecase.close(new CloseBusinessAccountRequest(businessId));

        assertThat(response).isEqualTo(new CloseBusinessAccountResponse(businessId, NOW));
        assertThat(world.businesses.find(businessId).orElseThrow().closedAt()).contains(NOW);
        assertThat(world.businesses.timesSaved(businessId)).isEqualTo(2);
        assertThat(world.publisher.published(AccountClosedIE.class))
                .containsExactly(new AccountClosedIE(businessId, "BUSINESS", NOW));
        assertThatThrownBy(() -> usecase.close(new CloseBusinessAccountRequest("none")))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
