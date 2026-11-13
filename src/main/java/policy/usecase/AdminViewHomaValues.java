package policy.usecase;

import java.util.Arrays;
import java.util.List;
import policy.domain.HomaValues;
import policy.domain.ValueChange;
import policy.usecase.dto.ViewHomaValuesRequest;
import policy.usecase.dto.ViewHomaValuesResponse;
import policy.usecase.dto.ViewHomaValuesResponse.Change;
import policy.usecase.dto.ViewHomaValuesResponse.Setting;
import policy.usecase.port.in.AdminHomaValuesViewer;
import policy.usecase.port.out.HomaValuesRepository;
import policy.usecase.port.out.ValueChangeRepository;

public final class AdminViewHomaValues implements AdminHomaValuesViewer {

    private final HomaValuesRepository values;
    private final ValueChangeRepository changes;

    public AdminViewHomaValues(HomaValuesRepository values, ValueChangeRepository changes) {
        this.values = values;
        this.changes = changes;
    }

    @Override
    public ViewHomaValuesResponse view(ViewHomaValuesRequest request) {
        HomaValues current = values.current().orElseGet(HomaValues::launch);
        List<Setting> settings = Arrays.stream(policy.domain.HomaValue.values())
                .map(value -> new Setting(value.name(), current.amountOf(value), value.unit().name()))
                .toList();
        List<Change> history = changes.all().stream().map(AdminViewHomaValues::viewOf).toList();
        return new ViewHomaValuesResponse(current.version(), settings, history);
    }

    private static Change viewOf(ValueChange change) {
        return new Change(change.value().name(), change.from(), change.to(), change.version(), change.reason(),
                change.adminId(), change.changedAt());
    }
}
