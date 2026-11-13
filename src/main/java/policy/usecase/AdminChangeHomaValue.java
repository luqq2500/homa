package policy.usecase;

import java.time.Clock;
import java.time.Instant;
import policy.domain.HomaValue;
import policy.domain.HomaValues;
import policy.domain.ValueChange;
import policy.usecase.dto.ChangeHomaValueRequest;
import policy.usecase.dto.ChangeHomaValueResponse;
import policy.usecase.exception.UnknownHomaValueException;
import policy.usecase.port.in.AdminHomaValueChanger;
import policy.usecase.port.out.HomaValuesRepository;
import policy.usecase.port.out.PolicyIEPublisher;
import policy.usecase.port.out.ValueChangeRepository;

public final class AdminChangeHomaValue implements AdminHomaValueChanger {

    private final HomaValuesRepository values;
    private final ValueChangeRepository changes;
    private final PolicyIEPublisher publisher;
    private final Clock clock;

    public AdminChangeHomaValue(HomaValuesRepository values, ValueChangeRepository changes,
                                PolicyIEPublisher publisher, Clock clock) {
        this.values = values;
        this.changes = changes;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChangeHomaValueResponse change(ChangeHomaValueRequest request) {
        HomaValue value = valueCalled(request.value());
        Instant now = clock.instant();
        HomaValues before = values.current().orElseGet(HomaValues::launch);
        HomaValues after = before.changed(value, request.amount());
        ValueChange change = ValueChange.record(before, after, value, request.reason(), request.adminId(), now);

        values.save(after);
        changes.save(change);
        publisher.publish(HomaValuesEvents.valuesChanged(after, now));
        return new ChangeHomaValueResponse(after.version(), value.name(), after.amountOf(value));
    }

    private static HomaValue valueCalled(String name) {
        try {
            return HomaValue.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownHomaValueException(name);
        }
    }
}
