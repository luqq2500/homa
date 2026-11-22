package com.homa.customer.domain;

/**
 * No longer Customer's values. The suspension lengths (§13: 7 days, 30 days, or until lifted) belong to Support's
 * {@code SuspensionLength}, and Customer receives the length and its end in Support's decision. Nothing in Customer
 * reads these numbers; they stay only so the Homa-set values check compiles until it is repointed to Support.
 *
 * @deprecated the suspension lengths are Support's; remove once the Homa-set values check no longer reads them.
 */
@Deprecated(forRemoval = true)
public final class CustomerValues {

    private CustomerValues() {
    }

    /** §13 suspension lengths, owned by Support. */
    public static final int SHORT_SUSPENSION_DAYS = 7;

    /** §13 suspension lengths, owned by Support. */
    public static final int LONG_SUSPENSION_DAYS = 30;
}
