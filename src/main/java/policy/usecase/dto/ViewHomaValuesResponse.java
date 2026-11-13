package policy.usecase.dto;

import java.time.Instant;
import java.util.List;

public record ViewHomaValuesResponse(long version, List<Setting> settings, List<Change> changes) {

    public ViewHomaValuesResponse {
        settings = List.copyOf(settings);
        changes = List.copyOf(changes);
    }

    public record Setting(String value, long amount, String unit) {
    }

    public record Change(String value, long from, long to, long version, String reason, String adminId,
                         Instant changedAt) {
    }
}
