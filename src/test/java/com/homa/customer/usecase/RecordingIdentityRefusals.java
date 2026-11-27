package com.homa.customer.usecase;

import com.homa.customer.usecase.port.out.IdentityRefusal;
import com.homa.customer.usecase.port.out.IdentityRefusals;
import java.util.ArrayList;
import java.util.List;

/** Keeps every refused verification, in order. */
final class RecordingIdentityRefusals implements IdentityRefusals {

    private final List<IdentityRefusal> recorded = new ArrayList<>();

    @Override
    public void record(IdentityRefusal refusal) {
        recorded.add(refusal);
    }

    List<IdentityRefusal> recorded() {
        return List.copyOf(recorded);
    }
}
