package account.support;

import ie.port.AccountIE;
import java.util.ArrayList;
import java.util.List;
import account.usecase.port.out.AccountIEPublisher;

public final class RecordingAccountIEPublisher implements AccountIEPublisher {

    private final List<AccountIE> published = new ArrayList<>();

    @Override
    public void publish(AccountIE event) {
        published.add(event);
    }

    public List<AccountIE> published() {
        return List.copyOf(published);
    }

    /** The published events of one type, in order. */
    public <E extends AccountIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
