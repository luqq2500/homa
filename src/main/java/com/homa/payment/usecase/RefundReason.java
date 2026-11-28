package com.homa.payment.usecase;

/** Why Payment sent money to a customer. Its name is the reason {@code RefundPaidIE} carries. */
public enum RefundReason {
    /** The customer withdrew a Requested reservation; everything held went back (REQ-2). */
    WITHDRAWN,
    /** The provider declined a Requested reservation; everything held went back (REQ-3). */
    DECLINED,
    /** A Requested reservation passed its acceptance deadline; everything held went back (REQ-4). */
    EXPIRED,
    /** The refund line of a cancellation settlement (PAY-4). */
    CANCELLED,
    /** The difference when the customer accepted a smaller roster: the price came down, the discount kept (ROS-8). */
    SMALLER_ROSTER,
    /** What Homa pays on top of a full refund, from its own money (PAY-4). */
    COMPENSATION
}
