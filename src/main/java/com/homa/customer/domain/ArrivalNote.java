package com.homa.customer.domain;

/** What the team needs to know to reach the home, such as "Guardhouse: say unit B-12-3" (CUS-1). */
public record ArrivalNote(String value) {

    public ArrivalNote {
        Required.text(value, "an arrival note");
    }
}
