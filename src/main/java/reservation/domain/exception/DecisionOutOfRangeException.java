package reservation.domain.exception;

import kernel.Money;

public class DecisionOutOfRangeException extends RuntimeException {

    public DecisionOutOfRangeException(Money atStake) {
        super("The provider receives from RM0 up to what was at stake: " + atStake.sen() + " sen");
    }
}
