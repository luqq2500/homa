package account.domain;

import static account.support.AccountFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.DataRequestAlreadyHandledException;
import org.junit.jupiter.api.Test;

class DataRequestTest {

    @Test
    void aDataRequestWaitsToBeHandled() {
        DataRequest request = DataRequest.make("customer-1", DataRequestKind.COPY, NOW);

        assertThat(request.id()).isNotBlank().isNotEqualTo(DataRequest.make("customer-1", DataRequestKind.COPY, NOW).id());
        assertThat(request.accountId()).isEqualTo("customer-1");
        assertThat(request.kind()).isEqualTo(DataRequestKind.COPY);
        assertThat(request.requestedAt()).isEqualTo(NOW);
        assertThat(request.handling()).isEmpty();
    }

    @Test
    void handlingIsRecordedWithTheAdminNoteAndTime() {
        DataRequest request = DataRequest.make("customer-1", DataRequestKind.DELETE, NOW);

        request.handle("admin-1", "Deleted; payment records kept by law", NOW.plusSeconds(3600));

        assertThat(request.handling()).contains(
                new DataRequest.Handling("admin-1", "Deleted; payment records kept by law", NOW.plusSeconds(3600)));
    }

    @Test
    void aDataRequestIsHandledOnce() {
        DataRequest request = DataRequest.make("customer-1", DataRequestKind.SEE, NOW);
        request.handle("admin-1", "Sent", NOW);

        assertThatThrownBy(() -> request.handle("admin-2", "Sent again", NOW.plusSeconds(1)))
                .isInstanceOf(DataRequestAlreadyHandledException.class);
        assertThat(request.handling()).contains(new DataRequest.Handling("admin-1", "Sent", NOW));
    }
}
