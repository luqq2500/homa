package offering.domain.exception;

import java.util.List;

public class NotReadyToGoLiveException extends RuntimeException {

    public NotReadyToGoLiveException(List<String> missing) {
        super("Not ready to go live; still missing: " + missing);
    }
}
