package com.homa.customer.usecase.port.out;

/** Keeps the verifications Customer refused (CUS-10), in the handler's transaction. */
public interface IdentityRefusals {

    void record(IdentityRefusal refusal);
}
