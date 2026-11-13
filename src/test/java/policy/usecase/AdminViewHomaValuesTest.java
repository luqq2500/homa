package policy.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import policy.domain.HomaValue;
import policy.support.InMemoryHomaValuesRepository;
import policy.support.InMemoryValueChangeRepository;
import policy.support.RecordingPolicyIEPublisher;
import policy.usecase.dto.ChangeHomaValueRequest;
import policy.usecase.dto.ViewHomaValuesRequest;
import policy.usecase.dto.ViewHomaValuesResponse;
import policy.usecase.dto.ViewHomaValuesResponse.Change;
import policy.usecase.dto.ViewHomaValuesResponse.Setting;

class AdminViewHomaValuesTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");

    private final InMemoryHomaValuesRepository values = new InMemoryHomaValuesRepository();
    private final InMemoryValueChangeRepository changes = new InMemoryValueChangeRepository();
    private final AdminViewHomaValues usecase = new AdminViewHomaValues(values, changes);

    @Test
    void beforeAnyChangeTheAdminSeesTheLaunchValues() {
        ViewHomaValuesResponse response = usecase.view(new ViewHomaValuesRequest("admin-1"));

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.settings()).hasSize(HomaValue.values().length)
                .contains(new Setting("SERVICE_FEE", 15, "PERCENT"), new Setting("FEE_WINDOW", 1440, "MINUTES"),
                        new Setting("CHECK_IN_RADIUS", 300, "METRES"));
        assertThat(response.changes()).isEmpty();
    }

    @Test
    void afterAChangeTheAdminSeesTheNewSettingAndTheChangeThatMadeIt() {
        new AdminChangeHomaValue(values, changes, new RecordingPolicyIEPublisher(), Clock.fixed(NOW, ZoneOffset.UTC))
                .change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "Launch promotion"));

        ViewHomaValuesResponse response = usecase.view(new ViewHomaValuesRequest("admin-2"));

        assertThat(response.version()).isEqualTo(2);
        assertThat(response.settings()).contains(new Setting("SERVICE_FEE", 12, "PERCENT"));
        assertThat(response.changes())
                .containsExactly(new Change("SERVICE_FEE", 15, 12, 2, "Launch promotion", "admin-1", NOW));
    }
}
