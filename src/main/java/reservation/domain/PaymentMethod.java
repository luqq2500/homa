package reservation.domain;

/** Card and online banking (FPX) are the only payment methods; each reservation is paid with one of them. */
public enum PaymentMethod {
    CARD,
    FPX
}
