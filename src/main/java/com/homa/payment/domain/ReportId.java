package com.homa.payment.domain;

/** A report on a reservation, as Payment knows it. While one is open the payout waits (PAY-2, RPT-6). */
public record ReportId(String value) {

    public ReportId {
        Ids.requireText(value, "ReportId");
    }
}
