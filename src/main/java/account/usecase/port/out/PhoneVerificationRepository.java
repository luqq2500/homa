package account.usecase.port.out;

import account.domain.PhoneVerification;
import java.util.List;
import java.util.Optional;

public interface PhoneVerificationRepository {

    void save(PhoneVerification phoneVerification);

    Optional<PhoneVerification> find(String id);

    List<PhoneVerification> all();
}
