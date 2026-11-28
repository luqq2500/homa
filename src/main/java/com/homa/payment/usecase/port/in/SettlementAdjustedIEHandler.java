package com.homa.payment.usecase.port.in;

import com.homa.ie.events.SettlementAdjustedIE;

/**
 * A reservation's price came down after it was Reserved: Payment refunds the difference at once and lowers the
 * provider's payout by the same amount (ROS-8, PAY-7).
 */
public interface SettlementAdjustedIEHandler {

    void handle(SettlementAdjustedIE event);
}
